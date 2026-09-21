package se.sundsvall.supportmanagement.service.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.access.AccessScope;
import se.sundsvall.supportmanagement.service.access.NamespaceGrant;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getCallerIdentity;

/**
 * What a search may reach for the requesting user.
 * <p>
 * The index holds the errand together with resources that are guarded on their own, and fields that a role may be
 * kept from. Trimming those from the answer is not enough, since a query naming a field tells by hit or miss what the
 * field holds. So the user is held to the same grants for the query as for reading: the fields they may not read are
 * left out of what a free text search looks in, and a query naming one of them, or sorting on one, is refused.
 * <p>
 * Three things close a field. A resource the labels of the user do not reach (reporter access alone reaches it on the
 * user's own errands only, not across the namespace). A field their roles keep from them. And a key of a keyed field
 * their roles keep from them: for JSON parameters the keys are paths of their own and the other keys stay open, while
 * parameter values and tag values sit in fields shared by every key, which therefore close as a whole.
 * <p>
 * Errands are searched at full read, so an errand the user reaches at limited read only is not found. Errands the
 * user reported are found besides, and what a reporter may read of an errand the labels do not cover is narrower still,
 * so a query beyond the reporter fields is answered from the label covered errands alone.
 */
@Component
public class ErrandSearchAccess {

	static final String NOT_SEARCHABLE = "%s not searchable by user '%s'";
	static final String NOT_SORTABLE = "%s not sortable by user '%s'";
	static final String WILDCARD_NOT_SEARCHABLE = "A wildcard in a field name is not available to user '%s', who may not search every field of the errand";

	private static final String JSON_PARAMETERS = "jsonParameters.";

	// A field name is whatever comes right before a colon, outside quotes. What is inside quotes is a phrase.
	private static final Pattern QUOTED = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"");
	private static final Pattern FIELD = Pattern.compile("(?<![\\w.\\\\*?-])([\\w.\\\\*?-]+):");
	// The value of _exists_ is a field name too
	private static final String EXISTS = "_exists_";
	// A field with its value: a group in parentheses, a range in brackets or braces, or a single term
	private static final Pattern FIELDED_TERM = Pattern.compile("[\\w.\\\\*?-]+:(?:\\([^)]*\\)|\\[[^\\]]*\\]|\\{[^}]*\\}|\\S+)");
	private static final Pattern OPERATORS = Pattern.compile("\\b(?:AND|OR|NOT|TO)\\b|&&|\\|\\||[+\\-!()]");

	/**
	 * One closed field or start of field names, and why.
	 *
	 * @param name        the field, or the start of the names of the fields when ending in a dot
	 * @param openKeys    for a keyed field, the keys that stay open under the name, null when the whole name is closed
	 * @param description what is closed, for the answer to the client
	 */
	record Closed(String name, Set<String> openKeys, String description) {

		Optional<String> reasonToRefuse(final String field) {
			if (!matches(field)) {
				return Optional.empty();
			}
			if (isNull(openKeys)) {
				return Optional.of(description);
			}
			final var key = field.substring(name.length()).split("\\.", 2)[0];
			return openKeys.contains(key) ? Optional.empty() : Optional.of("Key '%s' of %s".formatted(key, description));
		}

		private boolean matches(final String field) {
			return name.endsWith(".") ? field.startsWith(name) : field.equals(name) || field.startsWith(name + ".");
		}
	}

	/**
	 * What the requesting user may search.
	 *
	 * @param errand   which errands the user reaches
	 * @param closed   what is closed on the errands the labels of the user cover, empty when everything is open
	 * @param reported what is closed on the errands the user reported and the labels do not cover, null when that route
	 *                 is not open beside the labels
	 */
	public record Access(AccessScope errand, List<Closed> closed, List<Closed> reported) {}

	/**
	 * What a search runs with once the query has been held to the access.
	 *
	 * @param scope  which errands to search
	 * @param fields the fields a word without a field is looked for in
	 */
	public record Plan(AccessScope scope, List<String> fields) {}

	public Access resolve(final NamespaceGrant grant) {
		if (!grant.enforced()) {
			return new Access(grant.scope(), List.of(), null);
		}

		final var closedResources = closedResources(grant);
		final var labelsOpen = nonNull(grant.labels()) && grant.labels().reachesAnything();

		final var covered = new ArrayList<>(closedResources);
		covered.addAll(closedFields(isNull(grant.labels()) ? null : grant.labels().readable()));
		final var reported = new ArrayList<>(closedResources);
		reported.addAll(closedFields(isNull(grant.reporter()) ? null : grant.reporter().readable()));

		// The reporter fields are the rule when they are the only route, and a second rule beside the labels otherwise
		if (!labelsOpen) {
			return new Access(grant.scope(), List.copyOf(reported), null);
		}
		return new Access(grant.scope(), List.copyOf(covered), nonNull(grant.reporter()) ? List.copyOf(reported) : null);
	}

	/**
	 * Holds the query and the sort to the access, and settles what the search runs with.
	 *
	 * @throws org.springframework.web.ErrorResponseException 403 when the query or the sort reaches what is closed
	 */
	public Plan plan(final String query, final Sort sort, final Access access) {
		verify(query, sort, access.closed());
		final var fields = searchableFields(access.closed());

		if (isNull(access.reported())) {
			return new Plan(access.errand(), fields);
		}

		// The user's own errands are searched along with the label covered ones only while the query keeps to what a
		// reporter may read; otherwise they are left out rather than searched beyond it. A word without a field is
		// looked for in every open field, so it keeps within the reporter fields only if all of them do.
		final var withinReporterFields = isNull(refusal(query, sort, access.reported()))
			&& (!hasFreeTerms(query) || fields.stream().allMatch(field -> reasonToRefuse(field, access.reported()).isEmpty()));
		final var scope = withinReporterFields ? access.errand() : new AccessScope(true, access.errand().allowedLabels(), null);
		return new Plan(scope, fields);
	}

	private static List<Closed> closedResources(final NamespaceGrant grant) {
		final var closed = new ArrayList<Closed>();
		for (final var resource : ProtectedResource.values()) {
			if (!resource.getSearchFields().isEmpty() && !grant.reaches(resource)) {
				resource.getSearchFields().forEach(field -> closed.add(new Closed(field, null, "Resource '%s'".formatted(resource.getPath()))));
			}
		}
		return closed;
	}

	/**
	 * What sent in readable fields close. A null map restricts nothing; a field the map does not carry is closed; a
	 * keyed field carrying keys keeps those keys open where the index can tell them apart.
	 */
	private static List<Closed> closedFields(final Map<ErrandField, Set<String>> readable) {
		if (isNull(readable)) {
			return List.of();
		}

		final var closed = new ArrayList<Closed>();
		for (final var field : ErrandField.values()) {
			final var keys = readable.get(field);
			final var description = "Field '%s'".formatted(field.getPropertyName());
			if (isNull(keys)) {
				field.getSearchFields().forEach(name -> closed.add(new Closed(name, null, description)));
			} else if (!keys.isEmpty()) {
				field.getSearchFields().forEach(name -> closed.add(new Closed(name, JSON_PARAMETERS.equals(name) ? keys : null, JSON_PARAMETERS.equals(name) ? description : description + " beyond its keys")));
			}
		}
		return closed;
	}

	private static List<String> searchableFields(final List<Closed> closed) {
		return ErrandSearchPredicates.DEFAULT_FIELDS.stream()
			.filter(field -> reasonToRefuse(field, closed).isEmpty())
			.toList();
	}

	private static void verify(final String query, final Sort sort, final List<Closed> closed) {
		final var refusal = refusal(query, sort, closed);
		if (nonNull(refusal)) {
			throw refusal;
		}
	}

	/**
	 * Why the query or the sort would be refused under sent in closure, or null when it would not.
	 */
	private static RuntimeException refusal(final String query, final Sort sort, final List<Closed> closed) {
		if (closed.isEmpty()) {
			return null;
		}

		// A sort names a property of the errand, which is held to what the property's field is held to
		for (final var order : sort) {
			final var reason = Stream.of(ErrandField.values())
				.filter(field -> field.getPropertyName().equals(order.getProperty()))
				.flatMap(field -> field.getSearchFields().stream())
				.map(field -> reasonToRefuse(field, closed))
				.flatMap(Optional::stream)
				.findFirst();
			if (reason.isPresent()) {
				return Problem.valueOf(FORBIDDEN, NOT_SORTABLE.formatted(reason.get(), getCallerIdentity()));
			}
		}

		if (isBlank(query)) {
			return null;
		}

		final var unquoted = QUOTED.matcher(query).replaceAll(" ");
		final var matcher = FIELD.matcher(unquoted);
		while (matcher.find()) {
			final var refused = refusalOf(matcher.group(1), closed);
			if (nonNull(refused)) {
				return refused;
			}
			if (EXISTS.equals(matcher.group(1))) {
				final var value = refusalOf(unquoted.substring(matcher.end()).split("[\\s()]", 2)[0], closed);
				if (nonNull(value)) {
					return value;
				}
			}
		}
		return null;
	}

	/**
	 * Whether the query holds a word that names no field: what is left once phrases, fielded terms and operators are
	 * taken out.
	 */
	static boolean hasFreeTerms(final String query) {
		if (isBlank(query)) {
			return false;
		}
		// A phrase stands in as a single word, so that a phrase given to a field stays with the field
		final var unquoted = QUOTED.matcher(query).replaceAll("phrase");
		final var unfielded = FIELDED_TERM.matcher(unquoted).replaceAll(" ");
		return !OPERATORS.matcher(unfielded).replaceAll(" ").isBlank();
	}

	private static RuntimeException refusalOf(final String rawField, final List<Closed> closed) {
		final var field = rawField.replace("\\", "");

		if (field.contains("*") || field.contains("?")) {
			return Problem.valueOf(FORBIDDEN, WILDCARD_NOT_SEARCHABLE.formatted(getCallerIdentity()));
		}

		return reasonToRefuse(field, closed)
			.map(reason -> (RuntimeException) Problem.valueOf(FORBIDDEN, NOT_SEARCHABLE.formatted(reason, getCallerIdentity())))
			.orElse(null);
	}

	private static Optional<String> reasonToRefuse(final String field, final List<Closed> closed) {
		return closed.stream()
			.map(rule -> rule.reasonToRefuse(field))
			.flatMap(Optional::stream)
			.findFirst();
	}
}

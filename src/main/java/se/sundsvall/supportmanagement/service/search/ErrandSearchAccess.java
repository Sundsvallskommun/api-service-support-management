package se.sundsvall.supportmanagement.service.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.AccessControlService.AccessScope;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static java.util.Map.entry;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getCallerIdentity;

/**
 * What a search may reach for the requesting user.
 * <p>
 * The index holds the errand together with resources that are guarded on their own: communications, decisions and so
 * on. Trimming those from the answer is not enough, since a query naming a field of theirs tells by hit or miss what
 * the field holds. So the user is held to the same grants for the query as for reading the resources: the fields of a
 * resource the user may not read are left out of what a free text search looks in, and a query naming one of them is
 * refused. Errands are searched at full read, so an errand the user reaches at limited read only is not found at all,
 * rather than found and trimmed.
 * <p>
 * A resource is readable in a search when the labels of the user reach it: reporter access, which reaches the
 * resource on the user's own errands only, is not enough to search the resource across the namespace.
 */
@Component
public class ErrandSearchAccess {

	static final String RESOURCE_NOT_SEARCHABLE = "Resource '%s' not searchable by user '%s'";
	static final String WILDCARD_NOT_SEARCHABLE = "A wildcard in a field name is not available to user '%s', who may not search every resource of the errand";

	/** The index fields of each resource that is guarded on its own, by the start of their names. */
	static final Map<ProtectedResource, List<String>> RESOURCE_FIELDS = Map.ofEntries(
		entry(ProtectedResource.COMMUNICATION, List.of("communications.")),
		entry(ProtectedResource.DECISION, List.of("decisions.")),
		entry(ProtectedResource.STATEMENT, List.of("statements.")),
		entry(ProtectedResource.INVESTIGATION, List.of("investigations.")),
		entry(ProtectedResource.MEASURE, List.of("measures.")),
		entry(ProtectedResource.PARAMETER, List.of("parameters.")),
		entry(ProtectedResource.JSON_PARAMETER, List.of("jsonParameters.", "jsonParametersText")),
		entry(ProtectedResource.ATTACHMENT, List.of("attachments.")));

	// A field name is whatever comes right before a colon, outside quotes. What is inside quotes is a phrase.
	private static final Pattern QUOTED = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"");
	private static final Pattern FIELD = Pattern.compile("(?<![\\w.\\\\*?-])([\\w.\\\\*?-]+):");
	// The value of _exists_ is a field name too
	private static final String EXISTS = "_exists_";

	private final AccessControlService accessControlService;

	public ErrandSearchAccess(final AccessControlService accessControlService) {
		this.accessControlService = accessControlService;
	}

	/**
	 * What the requesting user may search.
	 *
	 * @param errand         which errands the user reaches at full read
	 * @param closedFields   starts of the names of the fields the user may not search, empty when everything is open
	 * @param closedResource the resource each closed field start belongs to
	 */
	public record Access(AccessScope errand, Set<String> closedFields, Map<String, ProtectedResource> closedResource) {

		boolean everythingOpen() {
			return closedFields.isEmpty();
		}
	}

	public Access resolve(final String namespace, final String municipalityId, final Identifier user) {
		final var errand = accessControlService.accessScope(namespace, municipalityId, user, ProtectedResource.ERRAND, R);
		if (!errand.enforced()) {
			return new Access(errand, Set.of(), Map.of());
		}

		final var closedFields = new TreeSet<String>();
		final var closedResource = new HashMap<String, ProtectedResource>();
		RESOURCE_FIELDS.forEach((resource, fields) -> {
			final var scope = accessControlService.accessScope(namespace, municipalityId, user, resource, R);
			if (isNull(scope.allowedLabels()) || scope.allowedLabels().isEmpty()) {
				fields.forEach(field -> {
					closedFields.add(field);
					closedResource.put(field, resource);
				});
			}
		});
		return new Access(errand, Set.copyOf(closedFields), Map.copyOf(closedResource));
	}

	/**
	 * The fields a free text search looks in for sent in access: the default ones, less those of closed resources.
	 */
	public List<String> searchableFields(final Access access) {
		return ErrandSearchPredicates.DEFAULT_FIELDS.stream()
			.filter(field -> access.closedFields().stream().noneMatch(field::startsWith))
			.toList();
	}

	/**
	 * Refuses a query that names a field of a resource the user may not read, or that names fields by wildcard when
	 * there is such a resource, since a wildcard may stand for any of them.
	 *
	 * @throws org.springframework.web.ErrorResponseException 403 when the query reaches a closed resource
	 */
	public void verifyQuery(final String query, final Access access) {
		if (isBlank(query) || access.everythingOpen()) {
			return;
		}

		final var unquoted = QUOTED.matcher(query).replaceAll(" ");
		final var matcher = FIELD.matcher(unquoted);
		while (matcher.find()) {
			verifyField(matcher.group(1), access);
			if (EXISTS.equals(matcher.group(1))) {
				verifyField(unquoted.substring(matcher.end()).split("[\\s()]", 2)[0], access);
			}
		}
	}

	private static void verifyField(final String rawField, final Access access) {
		final var field = rawField.replace("\\", "");

		if (field.contains("*") || field.contains("?")) {
			throw Problem.valueOf(FORBIDDEN, WILDCARD_NOT_SEARCHABLE.formatted(getCallerIdentity()));
		}

		access.closedFields().stream()
			.filter(field::startsWith)
			.findFirst()
			.ifPresent(closed -> {
				throw Problem.valueOf(FORBIDDEN, RESOURCE_NOT_SEARCHABLE.formatted(access.closedResource().get(closed).getPath(), getCallerIdentity()));
			});
	}
}

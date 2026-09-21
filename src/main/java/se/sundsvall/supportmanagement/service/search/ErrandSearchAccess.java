package se.sundsvall.supportmanagement.service.search;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.service.access.AccessScope;
import se.sundsvall.supportmanagement.service.access.NamespaceGrant;
import se.sundsvall.supportmanagement.service.search.index.ErrandIndexModel;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getCallerIdentity;

/**
 * Holds a search to what the requesting user may read.
 * <p>
 * The index holds the errand together with resources that are guarded on their own, and fields that a role may be
 * kept from. Trimming those from the answer is not enough, since a query naming a field tells by hit or miss what the
 * field holds. So the user is held to the same grant for the query as for reading, see {@link NamespaceGrant}: the
 * fields they may not read are left out of what a free text search looks in, and a query naming one of them, or
 * sorting on one, is refused. {@link FieldClosure} says what is closed on a route, {@link QueryStringFields} what a
 * query names; this puts the two together.
 * <p>
 * Errands are searched at full read, so an errand the user reaches at limited read only is not found. Errands the
 * user reported are found besides, and what a reporter may read of an errand the labels do not cover is narrower
 * still, so a query beyond the reporter fields is answered from the label covered errands alone.
 */
@Component
public class ErrandSearchAccess {

	static final String NOT_SEARCHABLE = "%s not searchable by user '%s'";
	static final String NOT_SORTABLE = "%s not sortable by user '%s'";
	static final String WILDCARD_NOT_SEARCHABLE = "A wildcard in a field name is not available to user '%s', who may not search every field of the errand";

	/**
	 * What a search runs with once the query has been held to the grant.
	 *
	 * @param scope  which errands to search
	 * @param fields the fields a word without a field is looked for in
	 */
	public record Plan(AccessScope scope, List<String> fields) {}

	private final ErrandIndexModel index;

	public ErrandSearchAccess(final ErrandIndexModel index) {
		this.index = index;
	}

	/**
	 * Holds the query and the sort to the grant, and settles what the search runs with.
	 *
	 * @throws org.springframework.web.ErrorResponseException 403 when the query or the sort reaches what is closed
	 */
	public Plan plan(final String query, final Sort sort, final NamespaceGrant grant) {
		if (!grant.enforced()) {
			return new Plan(grant.scope(), index.textFields());
		}

		final var labelsOpen = nonNull(grant.labels()) && grant.labels().reachesAnything();
		final var reported = isNull(grant.reporter()) ? null : FieldClosure.of(grant, grant.reporter().readable());

		// The reporter fields are the rule when they are the only route
		if (!labelsOpen) {
			final var closure = isNull(reported) ? FieldClosure.of(grant, null) : reported;
			refusal(query, sort, closure).ifPresent(refusal -> {
				throw refusal;
			});
			return new Plan(grant.scope(), closure.open(index.textFields()));
		}

		final var covered = FieldClosure.of(grant, grant.labels().readable());
		refusal(query, sort, covered).ifPresent(refusal -> {
			throw refusal;
		});
		final var fields = covered.open(index.textFields());

		if (isNull(reported)) {
			return new Plan(grant.scope(), fields);
		}

		// The user's own errands are searched along with the label covered ones only while the query keeps to what a
		// reporter may read; otherwise they are left out rather than searched beyond it. A word without a field is
		// looked for in every open field, so it keeps within the reporter fields only if all of them do.
		final var withinReporterFields = refusal(query, sort, reported).isEmpty()
			&& (!QueryStringFields.hasFreeTerms(query) || fields.stream().allMatch(reported::allows));
		return new Plan(withinReporterFields ? grant.scope() : new AccessScope(true, grant.labels().labels(), null), fields);
	}

	/**
	 * Why the query or the sort would be refused under sent in closure, empty when it would not.
	 */
	private static Optional<RuntimeException> refusal(final String query, final Sort sort, final FieldClosure closure) {
		if (closure.isOpen()) {
			return Optional.empty();
		}

		// A sort names a property of the errand, which is held to what the property's fields are held to
		for (final var order : sort) {
			final var refused = Stream.of(ErrandField.values())
				.filter(field -> field.getPropertyName().equals(order.getProperty()))
				.flatMap(field -> field.getSearchFields().stream())
				.map(closure::refusal)
				.flatMap(Optional::stream)
				.findFirst();
			if (refused.isPresent()) {
				return Optional.of(Problem.valueOf(FORBIDDEN, NOT_SORTABLE.formatted(refused.get(), getCallerIdentity())));
			}
		}

		for (final var field : QueryStringFields.fieldNames(query)) {
			// A wildcard may stand for any field, closed ones included
			if (QueryStringFields.isWildcard(field)) {
				return Optional.of(Problem.valueOf(FORBIDDEN, WILDCARD_NOT_SEARCHABLE.formatted(getCallerIdentity())));
			}
			final var refused = closure.refusal(field);
			if (refused.isPresent()) {
				return Optional.of(Problem.valueOf(FORBIDDEN, NOT_SEARCHABLE.formatted(refused.get(), getCallerIdentity())));
			}
		}

		return Optional.empty();
	}
}

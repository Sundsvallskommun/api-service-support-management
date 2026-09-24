package se.sundsvall.supportmanagement.service.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.service.access.AccessScope;
import se.sundsvall.supportmanagement.service.access.NamespaceGrant;
import se.sundsvall.supportmanagement.service.search.index.ErrandIndexModel;

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
 * A grant reaches errands by several routes, and what may be read differs between them: an errand the labels cover at
 * read is searched by everything the roles of the user allow, one they cover at limited read only by what a limited
 * read exposes, one the user reported by the reporter fields of the namespace. So a search is a clause per route, each
 * with its own errands and its own fields, and one query can search an errand by its body and another by its title
 * alone. A route whose fields cannot answer the query is left out rather than refused, which is what keeps a hit or a
 * miss from saying anything about the errands it reaches; the query is refused only when no route can answer it.
 */
@Component
public class ErrandSearchAccess {

	static final String NOT_SEARCHABLE = "%s not searchable by user '%s'";
	static final String NOT_SORTABLE = "%s not sortable by user '%s'";
	static final String WILDCARD_NOT_SEARCHABLE = "A wildcard in a field name is not available to user '%s', who may not search every field of the errand";

	/**
	 * One part of a search: the errands it reaches and the fields a word without a field is looked for in there.
	 *
	 * @param scope    the errands the clause reaches
	 * @param excluded errands to leave out of it although the scope reaches them, because another route holds them at a
	 *                 level exposing something else. Null when there are none
	 * @param fields   the fields a word without a field is looked for in
	 */
	public record Clause(AccessScope scope, AccessScope excluded, List<String> fields) {}

	/**
	 * What a search runs with once the query has been held to the grant, one clause per route that can answer it.
	 */
	public record Plan(List<Clause> clauses) {}

	/** A route of the grant, before the query has been held to it. */
	private record Route(AccessScope scope, AccessScope excluded, FieldClosure closure) {}

	private final ErrandIndexModel index;

	public ErrandSearchAccess(final ErrandIndexModel index) {
		this.index = index;
	}

	/**
	 * Holds the query and the sort to the grant, and settles what the search runs with.
	 *
	 * @throws org.springframework.web.ErrorResponseException 403 when no route of the grant can answer the query
	 */
	public Plan plan(final String query, final Sort sort, final NamespaceGrant grant) {
		if (!grant.enforced()) {
			return new Plan(List.of(new Clause(grant.scope(), null, index.textFields())));
		}

		final var routes = routesOf(grant);
		final var clauses = new ArrayList<Clause>();

		for (final var route : routes) {
			if (answers(query, sort, route.closure())) {
				clauses.add(new Clause(route.scope(), route.excluded(), route.closure().openFields(index.textFields())));
			}
		}

		if (clauses.isEmpty()) {
			// The widest route comes first, so its refusal is the one naming what the user would most expect to search
			throw routes.stream()
				.map(route -> refusal(query, sort, route.closure()))
				.flatMap(Optional::stream)
				.findFirst()
				.orElseGet(() -> Problem.valueOf(FORBIDDEN, NOT_SEARCHABLE.formatted("The errands of this namespace are", getCallerIdentity())));
		}

		return new Plan(List.copyOf(clauses));
	}

	/**
	 * The routes a search may run on, widest first: the errands the labels cover at read, those they cover at limited
	 * read, and those the user reported. A route the grant does not open is left out, and so is one reaching nothing.
	 */
	private static List<Route> routesOf(final NamespaceGrant grant) {
		final var routes = new ArrayList<Route>();
		final var covered = nonNull(grant.labels()) && grant.labels().reachesAnything() ? NamespaceGrant.scopeOf(grant.labels()) : null;

		if (nonNull(covered)) {
			routes.add(new Route(covered, null, FieldClosure.of(grant.labels().resources(), grant.labels().readable())));
		}
		if (nonNull(grant.limitedLabels()) && grant.limitedLabels().reachesAnything()) {
			// The labels of a level are a subset of those below it, so the limited route reaches the covered errands as
			// well - and those are held at the level, not at limited read. Leaving them out is what keeps a limited read
			// from widening what may be searched of an errand the user holds in full.
			routes.add(new Route(NamespaceGrant.scopeOf(grant.limitedLabels()), covered, FieldClosure.of(grant.limitedLabels().resources(), grant.limitedLabels().readable())));
		}
		if (nonNull(grant.reporter())) {
			routes.add(new Route(grant.reporterScope(), null, FieldClosure.of(grant.reporter().resources(), grant.reporter().readable())));
		}

		// A grant reaching nothing at all still answers, with a search that finds nothing rather than a refusal
		return routes.isEmpty() ? List.of(new Route(grant.scope(), null, FieldClosure.of(Set.of(), null))) : routes;
	}

	/**
	 * Whether a route can answer the query and the sort. A word without a field is looked for in every field the route
	 * leaves open, so such a query is answered by every route; a query naming a field, or a sort on one, is answered
	 * only where that field is open.
	 */
	private static boolean answers(final String query, final Sort sort, final FieldClosure closure) {
		return refusal(query, sort, closure).isEmpty();
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

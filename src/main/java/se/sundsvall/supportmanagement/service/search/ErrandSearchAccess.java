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
 * sorting on one, is refused. {@link QueryScanner} says what a query names, {@link SearchableFields} what a route may
 * search; this puts the two together.
 * <p>
 * Both of those fail closed, which is the lesson of the shapes this got wrong before. A name the scanner cannot place
 * is refused rather than passed on, and a query holding a colon the scanner could not read at all is refused whole:
 * reading a query differently from the index that answers it is how a field gets searched without being granted.
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
	static final String NOT_GROUPABLE = "%s not groupable by user '%s'";
	static final String WILDCARD_NOT_SEARCHABLE = "A wildcard in a field name is not available to user '%s', who may not search every field of the errand";
	static final String NOT_READ = "The query holds a field reference that could not be read, which user '%s' may not have searched unchecked";

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
	private record Route(AccessScope scope, AccessScope excluded, SearchableFields fields) {}

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
		return plan(query, sort, null, grant);
	}

	/**
	 * Holds the query, the sort and the column a count groups by to the grant, and settles what the search runs with.
	 *
	 * @param  groupBy                                        the field of the errand a count groups by, null when it
	 *                                                        counts without grouping
	 * @throws org.springframework.web.ErrorResponseException 403 when no route of the grant can answer the query, or
	 *                                                        when a route that can may not read the group column
	 */
	public Plan plan(final String query, final Sort sort, final ErrandField groupBy, final NamespaceGrant grant) {
		if (!grant.enforced()) {
			return new Plan(List.of(new Clause(grant.scope(), null, index.textFields())));
		}

		// Read once, whatever the grant turns out to reach: what the query names does not depend on who is asking
		final var scan = QueryScanner.scan(query);
		final var routes = routesOf(grant);
		final var clauses = new ArrayList<Clause>();
		final var answering = new ArrayList<Route>();

		for (final var route : routes) {
			if (refusal(scan, sort, route.fields()).isEmpty()) {
				answering.add(route);
				clauses.add(new Clause(route.scope(), route.excluded(), route.fields().openFields(index.textFields())));
			}
		}

		if (clauses.isEmpty()) {
			// The widest route comes first, so its refusal is the one naming what the user would most expect to search
			throw routes.stream()
				.map(route -> refusal(scan, sort, route.fields()))
				.flatMap(Optional::stream)
				.findFirst()
				.orElseGet(() -> Problem.valueOf(FORBIDDEN, NOT_SEARCHABLE.formatted("The errands of this namespace are", getCallerIdentity())));
		}

		verifyGroupable(groupBy, answering);
		return new Plan(List.copyOf(clauses));
	}

	/**
	 * Holds the group column to every route that answers the query.
	 * <p>
	 * Grouping a count by a column reads that column of every errand counted, so a route that may not read it may not be
	 * counted by it. A route already left out of the plan is not asked: it contributes no errand, so it can hide nothing.
	 * <p>
	 * Refused whole rather than counted over the routes that may, because the alternative answers with buckets adding up
	 * to less than the count printed beside them, and nothing on the endpoint could explain the difference.
	 */
	private static void verifyGroupable(final ErrandField groupBy, final List<Route> answering) {
		if (isNull(groupBy)) {
			return;
		}

		final var refused = answering.stream()
			.map(route -> route.fields().wholeFieldRefusal(groupBy))
			.flatMap(Optional::stream)
			.findFirst();
		if (refused.isPresent()) {
			throw Problem.valueOf(FORBIDDEN, NOT_GROUPABLE.formatted(refused.get(), getCallerIdentity()));
		}
	}

	/**
	 * The routes a search may run on, widest first: the errands the labels cover at read, those they cover at limited
	 * read, and those the user reported. A route the grant does not open is left out, and so is one reaching nothing.
	 */
	private static List<Route> routesOf(final NamespaceGrant grant) {
		final var routes = new ArrayList<Route>();
		final var covered = nonNull(grant.labels()) && grant.labels().reachesAnything() ? NamespaceGrant.scopeOf(grant.labels()) : null;

		if (nonNull(covered)) {
			routes.add(new Route(covered, null, SearchableFields.of(grant.labels().resources(), grant.labels().readable())));
		}
		if (nonNull(grant.limitedLabels()) && grant.limitedLabels().reachesAnything()) {
			// The labels of a level are a subset of those below it, so the limited route reaches the covered errands as
			// well - and those are held at the level, not at limited read. Leaving them out is what keeps a limited read
			// from widening what may be searched of an errand the user holds in full.
			routes.add(new Route(NamespaceGrant.scopeOf(grant.limitedLabels()), covered,
				SearchableFields.of(grant.limitedLabels().resources(), grant.limitedLabels().readable())));
		}
		if (nonNull(grant.reporter())) {
			routes.add(new Route(grant.reporterScope(), null, SearchableFields.of(grant.reporter().resources(), grant.reporter().readable())));
		}

		// A grant reaching nothing at all still answers, with a search that finds nothing rather than a refusal
		return routes.isEmpty() ? List.of(new Route(grant.scope(), null, SearchableFields.of(Set.of(), null))) : routes;
	}

	/**
	 * Why the query or the sort would be refused on a route, empty when it would not.
	 */
	private static Optional<RuntimeException> refusal(final QueryScanner.Scan scan, final Sort sort, final SearchableFields fields) {
		if (fields.unrestricted()) {
			// Nothing is held back here, so nothing the query names can be held back either
			return Optional.empty();
		}

		if (!scan.isFullyRead()) {
			return Optional.of(Problem.valueOf(FORBIDDEN, NOT_READ.formatted(getCallerIdentity())));
		}

		// A sort names a property, which belongs to a field of the errand: ordering by it says as much about that field as
		// searching it does. Every field offering the property is asked, which is what holds a sort on 'category' to the
		// classification it belongs to rather than to a field named after it
		for (final var order : sort) {
			final var refused = Stream.of(ErrandField.values())
				.filter(field -> field.getSortField(order.getProperty()).isPresent())
				.map(fields::wholeFieldRefusal)
				.flatMap(Optional::stream)
				.findFirst();
			if (refused.isPresent()) {
				return Optional.of(Problem.valueOf(FORBIDDEN, NOT_SORTABLE.formatted(refused.get(), getCallerIdentity())));
			}
		}

		for (final var reference : scan.fields()) {
			// A wildcard stands for names nobody enumerated, so it belongs to a route that is held to nothing
			if (QueryScanner.isWildcard(reference.name())) {
				return Optional.of(Problem.valueOf(FORBIDDEN, WILDCARD_NOT_SEARCHABLE.formatted(getCallerIdentity())));
			}
			final var refused = fields.refusal(reference.name());
			if (refused.isPresent()) {
				return Optional.of(Problem.valueOf(FORBIDDEN, NOT_SEARCHABLE.formatted(refused.get(), getCallerIdentity())));
			}
		}

		return Optional.empty();
	}
}

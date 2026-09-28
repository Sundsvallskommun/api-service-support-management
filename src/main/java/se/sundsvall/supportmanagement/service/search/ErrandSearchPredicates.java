package se.sundsvall.supportmanagement.service.search;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Set;
import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.search.ErrandIndex;
import se.sundsvall.supportmanagement.service.access.AccessScope;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * The predicates an errand search is made of: what the client asked for, and what the client is allowed to reach.
 */
@Component
public class ErrandSearchPredicates {

	static final String MUNICIPALITY_ID_FIELD = ErrandIndex.MUNICIPALITY_ID;
	static final String NAMESPACE_FIELD = ErrandIndex.NAMESPACE;
	static final String REPORTER_USER_ID_FIELD = ErrandIndex.REPORTER_USER_ID;
	static final String ACCESS_LABEL_ID_FIELD = ErrandIndex.ACCESS_LABEL_ID;

	/**
	 * What the client asked for. A blank query matches everything, so that a client can page through a namespace sorted
	 * the way it likes without inventing a query. Anything else is a Lucene query string, parsed by OpenSearch, with every
	 * word required unless the query says otherwise.
	 *
	 * @param f      the factory
	 * @param query  the query string
	 * @param fields the fields a word without a field is looked for in
	 */
	public SearchPredicate query(final SearchPredicateFactory f, final String query, final List<String> fields) {
		if (isBlank(query)) {
			return f.matchAll().toPredicate();
		}

		// A route may leave open no field that a word without one is looked for in: what a role allows may be the status
		// alone, which is searched by name and not by word. The field every errand of the search is filtered on stands in
		// for the list then, so that a word matches nothing by itself while the fielded terms, the disjunctions and the
		// negations of the query still compose - answering "status:new OR vatten" with the new errands rather than with
		// nothing, which is what refusing the whole clause did.
		return f.queryString()
			.fields(fields.isEmpty() ? new String[] {
				MUNICIPALITY_ID_FIELD
			} : fields.toArray(String[]::new))
			.matching(query)
			.defaultOperator(BooleanOperator.AND)
			.toPredicate();
	}

	/**
	 * What a search asks the index, once the query has been held to the grant: the errands of one route together with
	 * the query over the fields that route leaves open, any of the routes answering.
	 * <p>
	 * Routes may reach the same errand, since the labels of a level are a subset of those of every level below it. The
	 * document is returned once whichever clauses matched it, and each clause only matched on fields readable on its own
	 * errands, so overlapping says nothing the user may not know.
	 *
	 * @param clauses what the search runs with, see {@link ErrandSearchAccess.Plan}
	 */
	public SearchPredicate clauses(final SearchPredicateFactory f, final List<ErrandSearchAccess.Clause> clauses, final String query, final String namespace, final String municipalityId) {
		if (clauses.size() == 1) {
			return clause(f, clauses.getFirst(), query, namespace, municipalityId).toPredicate();
		}

		final var union = f.or();
		clauses.forEach(clause -> union.add(clause(f, clause, query, namespace, municipalityId)));
		return union.toPredicate();
	}

	private PredicateFinalStep clause(final SearchPredicateFactory f, final ErrandSearchAccess.Clause clause, final String query, final String namespace, final String municipalityId) {
		final var predicate = f.bool()
			.filter(access(f, clause.scope(), namespace, municipalityId))
			.must(query(f, query, clause.fields()));

		if (nonNull(clause.excluded())) {
			predicate.mustNot(access(f, clause.excluded(), namespace, municipalityId));
		}

		return predicate;
	}

	/**
	 * The errands of one namespace, which is the index side of the tenancy the database keeps with the same two columns.
	 */
	public SearchPredicate tenant(final SearchPredicateFactory f, final String namespace, final String municipalityId) {
		return f.and(
			f.match().field(MUNICIPALITY_ID_FIELD).matching(municipalityId),
			f.match().field(NAMESPACE_FIELD).matching(namespace))
			.toPredicate();
	}

	/**
	 * What the client is allowed to reach, the same rule as
	 * {@link se.sundsvall.supportmanagement.service.util.SpecificationBuilder#hasAllowedMetadataLabels} and
	 * {@link se.sundsvall.supportmanagement.service.util.SpecificationBuilder#isReportedBy} put together, said in
	 * terms the index can answer.
	 * <p>
	 * The labels rule is "every access label of the errand is among those allowed". An index cannot ask whether all values
	 * of a field lie within a set, but it can ask how many of them do, and the number to reach is written beside them by
	 * {@link se.sundsvall.supportmanagement.integration.db.search.AccessLabelCountBinder}: as many as the errand carries.
	 * Asked this way the filter needs nothing but the labels the user holds - no list of the namespace's labels, and
	 * nothing cached that could be out of date - and a label nobody has heard of yet keeps an errand out rather than
	 * letting it through, which is the direction a filter should fail in.
	 * <p>
	 * An errand carrying no access labels is reached by everyone holding any label, as in the database, and is asked for
	 * separately: a count of none satisfies no covering query, whatever it is counted against. A user holding no labels
	 * reaches nothing at all, unlabelled errands included, which is the database's answer too.
	 */
	public SearchPredicate access(final SearchPredicateFactory f, final AccessScope scope, final String namespace, final String municipalityId) {
		if (!scope.enforced()) {
			return f.matchAll().toPredicate();
		}

		final var routes = f.or();

		if (!isNull(scope.allowedLabels())) {
			routes.add(withinAllowedLabels(f, scope.allowedLabelIds()));
		}

		if (!isNull(scope.reporterAdAccount())) {
			routes.add(f.match().field(REPORTER_USER_ID_FIELD).matching(scope.reporterAdAccount()));
		}

		return routes.hasClause() ? routes.toPredicate() : f.matchNone().toPredicate();
	}

	/**
	 * Every access label of the errand among those allowed, counted against how many it carries, or no access labels at
	 * all. Written as the index takes it, since the query DSL knows no covering query.
	 */
	private PredicateFinalStep withinAllowedLabels(final SearchPredicateFactory f, final Set<String> allowedLabelIds) {
		if (allowedLabelIds.isEmpty()) {
			// The specification reaches no errand at all for a user holding no labels, unlabelled errands included
			return f.matchNone();
		}

		return f.or(
			f.extension(ElasticsearchExtension.get()).fromJson(everyLabelAllowed(allowedLabelIds)),
			f.extension(ElasticsearchExtension.get()).fromJson(noLabelsAtAll()));
	}

	/**
	 * At least as many of the errand's access labels among those allowed as the errand carries, which is every one of
	 * them.
	 */
	static JsonObject everyLabelAllowed(final Set<String> allowedLabelIds) {
		final var terms = new JsonArray();
		allowedLabelIds.stream().sorted().forEach(terms::add);

		final var covering = new JsonObject();
		covering.add("terms", terms);
		covering.addProperty("minimum_should_match_field", ErrandIndex.ACCESS_LABEL_COUNT);

		final var byField = new JsonObject();
		byField.add(ACCESS_LABEL_ID_FIELD, covering);

		final var query = new JsonObject();
		query.add("terms_set", byField);
		return query;
	}

	/** An errand carrying no access labels, which a covering query answers for no set of labels. */
	static JsonObject noLabelsAtAll() {
		final var count = new JsonObject();
		count.addProperty(ErrandIndex.ACCESS_LABEL_COUNT, 0);

		final var query = new JsonObject();
		query.add("term", count);
		return query;
	}

}

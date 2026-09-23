package se.sundsvall.supportmanagement.service.search;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.search.ErrandIndex;
import se.sundsvall.supportmanagement.service.access.AccessScope;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;
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

	private final MetadataLabelRepository metadataLabelRepository;

	public ErrandSearchPredicates(final MetadataLabelRepository metadataLabelRepository) {
		this.metadataLabelRepository = metadataLabelRepository;
	}

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
		return f.queryString()
			.fields(fields.toArray(String[]::new))
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
	 * The labels rule is "no access label outside the allowed set". An index cannot ask whether all values of a field lie
	 * within a set, but it can ask whether any value lies within the complement of it, and the complement is known since
	 * the labels of a namespace are: every label of the namespace that is not allowed is disallowed, and an errand
	 * carrying any of them is out. An errand without access labels carries nothing disallowed and is reached by everyone,
	 * as in the database.
	 * <p>
	 * One difference remains: an errand carrying the id of a label that no longer exists in the metadata is hidden by the
	 * database, since that id is not in the allowed set, but reached here, since it is not in the disallowed set either.
	 * Labels are not removed while errands carry them, so this is not expected to matter.
	 */
	public SearchPredicate access(final SearchPredicateFactory f, final AccessScope scope, final String namespace, final String municipalityId) {
		if (!scope.enforced()) {
			return f.matchAll().toPredicate();
		}

		final var routes = f.or();

		if (!isNull(scope.allowedLabels())) {
			routes.add(withinAllowedLabels(f, scope.allowedLabelIds(), namespace, municipalityId));
		}

		if (!isNull(scope.reporterAdAccount())) {
			routes.add(f.match().field(REPORTER_USER_ID_FIELD).matching(scope.reporterAdAccount()));
		}

		return routes.hasClause() ? routes.toPredicate() : f.matchNone().toPredicate();
	}

	private PredicateFinalStep withinAllowedLabels(final SearchPredicateFactory f, final Set<String> allowedLabelIds, final String namespace, final String municipalityId) {
		if (allowedLabelIds.isEmpty()) {
			return f.matchNone();
		}

		final Set<String> disallowedLabelIds = new HashSet<>(labelIdsOf(namespace, municipalityId));
		disallowedLabelIds.removeAll(allowedLabelIds);

		if (disallowedLabelIds.isEmpty()) {
			return f.matchAll();
		}

		return f.not(f.terms().field(ACCESS_LABEL_ID_FIELD).matchingAny(disallowedLabelIds));
	}

	private Set<String> labelIdsOf(final String namespace, final String municipalityId) {
		return metadataLabelRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId).stream()
			.map(MetadataLabelEntity::getId)
			.collect(toSet());
	}
}

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
import se.sundsvall.supportmanagement.service.AccessControlService.AccessScope;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * The predicates an errand search is made of: what the client asked for, and what the client is allowed to reach.
 */
@Component
public class ErrandSearchPredicates {

	/**
	 * Where a query without a field looks. Every text field of the errand and what hangs off it, plus the identifiers a
	 * user is likely to paste into a search box.
	 */
	static final List<String> DEFAULT_FIELDS = List.of(
		"errandNumber", "title", "description", "contactReasonDescription",
		"externalTags.value",
		"stakeholders.externalId", "stakeholders.firstName", "stakeholders.lastName", "stakeholders.organizationName", "stakeholders.address",
		"stakeholders.city", "stakeholders.careOf", "stakeholders.contactChannels.value", "stakeholders.parameters.values",
		"parameters.values", "jsonParametersText", "attachments.fileName",
		"phases.phase.displayName",
		"measures.title", "measures.description", "measures.goal", "measures.acceptMotivation", "measures.resultText", "measures.jsonParametersText",
		"decisions.title", "decisions.description", "decisions.legalBasis", "decisions.justification", "decisions.jsonParametersText",
		"statements.title", "statements.description", "statements.counterpartyName", "statements.question", "statements.responseText", "statements.jsonParametersText",
		"investigations.title", "investigations.description", "investigations.summary", "investigations.conclusion", "investigations.recommendationMotivation",
		"investigations.jsonParametersText",
		"communications.subject", "communications.messageBody");

	static final String MUNICIPALITY_ID_FIELD = "municipalityId";
	static final String NAMESPACE_FIELD = "namespace";
	static final String REPORTER_USER_ID_FIELD = "reporterUserId";
	static final String ACCESS_LABEL_ID_FIELD = "accessLabels.metadataLabelId";

	private final MetadataLabelRepository metadataLabelRepository;

	public ErrandSearchPredicates(final MetadataLabelRepository metadataLabelRepository) {
		this.metadataLabelRepository = metadataLabelRepository;
	}

	/**
	 * What the client asked for. A blank query matches everything, so that a client can page through a namespace sorted
	 * the way it likes without inventing a query. Anything else is a Lucene query string, parsed by OpenSearch, with every
	 * word required unless the query says otherwise.
	 */
	public SearchPredicate query(final SearchPredicateFactory f, final String query) {
		if (isBlank(query)) {
			return f.matchAll().toPredicate();
		}
		return f.queryString()
			.fields(DEFAULT_FIELDS.toArray(String[]::new))
			.matching(query)
			.defaultOperator(BooleanOperator.AND)
			.toPredicate();
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

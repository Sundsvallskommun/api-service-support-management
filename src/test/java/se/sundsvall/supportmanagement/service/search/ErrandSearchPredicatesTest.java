package se.sundsvall.supportmanagement.service.search;

import java.util.List;
import java.util.Set;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.MatchAllPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchNonePredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.NotPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.TermsPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.TermsPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.TermsPredicateOptionsStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.service.access.AccessScope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The search DSL is a chain of self-typed steps, so each link is stubbed by hand. What matters is which predicate is
 * asked for and with what, not the shape of the chain.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
	"unchecked", "rawtypes"
})
class ErrandSearchPredicatesTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private SearchPredicateFactory factoryMock;

	@Mock
	private SearchPredicate predicateMock;

	@Mock
	private org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory elasticsearchFactoryMock;

	@Mock
	private MatchAllPredicateOptionsStep matchAllMock;

	@Mock
	private MatchNonePredicateFinalStep matchNoneMock;

	@Mock
	private SimpleBooleanPredicateClausesStep orMock;

	@Mock
	private SimpleBooleanPredicateOptionsStep andMock;

	@Mock
	private NotPredicateFinalStep notMock;

	@Mock
	private MatchPredicateFieldStep matchFieldStepMock;

	@Mock
	private MatchPredicateFieldMoreStep matchFieldMoreStepMock;

	@Mock
	private MatchPredicateOptionsStep matchOptionsMock;

	@Mock
	private TermsPredicateFieldStep termsFieldStepMock;

	@Mock
	private TermsPredicateFieldMoreStep termsFieldMoreStepMock;

	@Mock
	private TermsPredicateOptionsStep termsOptionsMock;

	@Mock
	private QueryStringPredicateFieldStep queryStringFieldStepMock;

	@Mock
	private QueryStringPredicateFieldMoreStep queryStringFieldMoreStepMock;

	@Mock
	private QueryStringPredicateOptionsStep queryStringOptionsMock;

	private ErrandSearchPredicates predicates() {
		return new ErrandSearchPredicates();
	}

	@Test
	void blankQueryMatchesAll() {
		when(factoryMock.matchAll()).thenReturn(matchAllMock);
		when(matchAllMock.toPredicate()).thenReturn(predicateMock);

		assertThat(predicates().query(factoryMock, " ", List.of("title"))).isSameAs(predicateMock);
		assertThat(predicates().query(factoryMock, null, List.of("title"))).isSameAs(predicateMock);
		verify(factoryMock, never()).queryString();
	}

	@Test
	void queryIsAQueryStringOverTheGivenFieldsWithEveryWordRequired() {
		when(factoryMock.queryString()).thenReturn(queryStringFieldStepMock);
		when(queryStringFieldStepMock.fields(any(String[].class))).thenReturn(queryStringFieldMoreStepMock);
		when(queryStringFieldMoreStepMock.matching(anyString())).thenReturn(queryStringOptionsMock);
		when(queryStringOptionsMock.defaultOperator(any())).thenReturn(queryStringOptionsMock);
		when(queryStringOptionsMock.toPredicate()).thenReturn(predicateMock);

		assertThat(predicates().query(factoryMock, "vatten status:new", List.of("title", "description"))).isSameAs(predicateMock);

		verify(queryStringFieldStepMock).fields("title", "description");
		verify(queryStringFieldMoreStepMock).matching("vatten status:new");
		verify(queryStringOptionsMock).defaultOperator(BooleanOperator.AND);
	}

	@Test
	void tenantIsTheNamespaceAndTheMunicipality() {
		when(factoryMock.match()).thenReturn(matchFieldStepMock);
		when(matchFieldStepMock.field(anyString())).thenReturn(matchFieldMoreStepMock);
		when(matchFieldMoreStepMock.matching(any())).thenReturn(matchOptionsMock);
		when(factoryMock.and(any(PredicateFinalStep.class), any(PredicateFinalStep.class))).thenReturn(andMock);
		when(andMock.toPredicate()).thenReturn(predicateMock);

		assertThat(predicates().tenant(factoryMock, NAMESPACE, MUNICIPALITY_ID)).isSameAs(predicateMock);

		verify(matchFieldStepMock).field(ErrandSearchPredicates.MUNICIPALITY_ID_FIELD);
		verify(matchFieldStepMock).field(ErrandSearchPredicates.NAMESPACE_FIELD);
		verify(matchFieldMoreStepMock).matching(MUNICIPALITY_ID);
		verify(matchFieldMoreStepMock).matching(NAMESPACE);
	}

	@Test
	void accessWhenNotEnforced() {
		when(factoryMock.matchAll()).thenReturn(matchAllMock);
		when(matchAllMock.toPredicate()).thenReturn(predicateMock);

		assertThat(predicates().access(factoryMock, new AccessScope(false, null, null), NAMESPACE, MUNICIPALITY_ID)).isSameAs(predicateMock);

	}

	@Test
	void accessWhenNoRouteIsOpen() {
		when(factoryMock.or()).thenReturn(orMock);
		when(orMock.hasClause()).thenReturn(false);
		when(factoryMock.matchNone()).thenReturn(matchNoneMock);
		when(matchNoneMock.toPredicate()).thenReturn(predicateMock);

		assertThat(predicates().access(factoryMock, new AccessScope(true, null, null), NAMESPACE, MUNICIPALITY_ID)).isSameAs(predicateMock);

		verify(orMock, never()).add(any(PredicateFinalStep.class));
	}

	/**
	 * A route leaving no field open for a word that names none: the word is looked for in a field no errand carries,
	 * where it matches nothing whatever it is, so the fielded terms and the operators around it still compose.
	 */
	@Test
	void aWordWithNoFieldToLookInSearchesAFieldNoErrandCarries() {
		when(factoryMock.queryString()).thenReturn(queryStringFieldStepMock);
		when(queryStringFieldStepMock.fields(any(String[].class))).thenReturn(queryStringFieldMoreStepMock);
		when(queryStringFieldMoreStepMock.matching(anyString())).thenReturn(queryStringOptionsMock);
		when(queryStringOptionsMock.defaultOperator(BooleanOperator.AND)).thenReturn(queryStringOptionsMock);
		when(queryStringOptionsMock.toPredicate()).thenReturn(predicateMock);

		assertThat(predicates().query(factoryMock, "vatten", List.of())).isSameAs(predicateMock);

		verify(queryStringFieldStepMock).fields(ErrandSearchPredicates.NO_OPEN_FIELD);
		verify(factoryMock, never()).matchNone();
	}

	/**
	 * The same route answering a query that names its fields: those were held to what the route may read, so the query
	 * runs, with the field no errand carries standing in for the list it has no use for.
	 */
	@Test
	void aQueryNamingItsOwnFieldsRunsWithNoFieldsOfItsOwn() {
		when(factoryMock.queryString()).thenReturn(queryStringFieldStepMock);
		when(queryStringFieldStepMock.fields(any(String[].class))).thenReturn(queryStringFieldMoreStepMock);
		when(queryStringFieldMoreStepMock.matching(anyString())).thenReturn(queryStringOptionsMock);
		when(queryStringOptionsMock.defaultOperator(BooleanOperator.AND)).thenReturn(queryStringOptionsMock);
		when(queryStringOptionsMock.toPredicate()).thenReturn(predicateMock);

		assertThat(predicates().query(factoryMock, "status:new", List.of())).isSameAs(predicateMock);

		verify(queryStringFieldStepMock).fields(ErrandSearchPredicates.NO_OPEN_FIELD);
		verify(factoryMock, never()).matchNone();
	}

	/**
	 * Every access label of the errand among those allowed, counted against how many it carries, or no access labels at
	 * all. The two are written as the index takes them, so what they say is asserted of the JSON rather than of a chain of
	 * mocks; ErrandSearchIT holds the rule itself against a real OpenSearch.
	 */
	@Test
	void accessThroughLabelsAsksThatEveryLabelOfTheErrandIsAllowed() {
		assertThat(ErrandSearchPredicates.everyLabelAllowed(Set.of("allowed-2", "allowed-1")).toString())
			.isEqualTo("{\"terms_set\":{\"accessLabels.metadataLabelId\":{\"terms\":[\"allowed-1\",\"allowed-2\"],\"minimum_should_match_field\":\"accessLabelCount\"}}}");
	}

	/**
	 * An errand carrying no access labels satisfies no covering query, whatever it is counted against, and is reached by
	 * everyone holding a label, as in the database.
	 */
	@Test
	void anErrandWithoutAccessLabelsIsAskedForOnItsOwn() {
		assertThat(ErrandSearchPredicates.noLabelsAtAll().toString()).isEqualTo("{\"term\":{\"accessLabelCount\":0}}");
	}

	@Test
	void accessThroughNoLabelsAtAllReachesNothingThroughLabels() {
		when(factoryMock.or()).thenReturn(orMock);
		when(orMock.hasClause()).thenReturn(true);
		when(orMock.toPredicate()).thenReturn(predicateMock);
		when(factoryMock.matchNone()).thenReturn(matchNoneMock);

		predicates().access(factoryMock, new AccessScope(true, Set.of(), null), NAMESPACE, MUNICIPALITY_ID);

		verify(orMock).add(matchNoneMock);
	}

	/**
	 * Both routes of a scope: the errands the labels reach and the errands the user reported, either answering.
	 */
	@Test
	void accessThroughReportingAndLabels() {
		final var allowed = label("allowed-1");
		when(factoryMock.or()).thenReturn(orMock);
		when(orMock.hasClause()).thenReturn(true);
		when(orMock.toPredicate()).thenReturn(predicateMock);
		when(factoryMock.match()).thenReturn(matchFieldStepMock);
		when(matchFieldStepMock.field(ErrandSearchPredicates.REPORTER_USER_ID_FIELD)).thenReturn(matchFieldMoreStepMock);
		when(matchFieldMoreStepMock.matching("rep01ort")).thenReturn(matchOptionsMock);
		// The labels branch is written as the index takes it, so the extension hands back what it is given
		when(factoryMock.extension(any(org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension.class))).thenReturn(elasticsearchFactoryMock);
		when(elasticsearchFactoryMock.fromJson(any(com.google.gson.JsonObject.class))).thenReturn(notMock);
		when(factoryMock.or(any(PredicateFinalStep.class), any(PredicateFinalStep.class))).thenReturn(andMock);

		assertThat(predicates().access(factoryMock, new AccessScope(true, Set.of(allowed), "rep01ort"), NAMESPACE, MUNICIPALITY_ID)).isSameAs(predicateMock);

		// The labels branch is written as the index takes it, so what is asserted of it is the JSON above
		verify(orMock).add(matchOptionsMock);
	}

	private static MetadataLabelEntity label(final String id) {
		return MetadataLabelEntity.create().withId(id);
	}
}

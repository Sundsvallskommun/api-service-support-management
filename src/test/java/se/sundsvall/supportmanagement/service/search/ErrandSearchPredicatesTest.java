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
import se.sundsvall.supportmanagement.service.MetadataService;
import se.sundsvall.supportmanagement.service.access.AccessScope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
	private MetadataService metadataServiceMock;

	@Mock
	private SearchPredicate predicateMock;

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
		return new ErrandSearchPredicates(metadataServiceMock);
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

		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void accessWhenNoRouteIsOpen() {
		when(factoryMock.or()).thenReturn(orMock);
		when(orMock.hasClause()).thenReturn(false);
		when(factoryMock.matchNone()).thenReturn(matchNoneMock);
		when(matchNoneMock.toPredicate()).thenReturn(predicateMock);

		assertThat(predicates().access(factoryMock, new AccessScope(true, null, null), NAMESPACE, MUNICIPALITY_ID)).isSameAs(predicateMock);

		verify(orMock, never()).add(any(PredicateFinalStep.class));
		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void accessThroughLabelsExcludesTheLabelsTheUserLacks() {
		final var allowed = label("allowed-1");
		final var alsoAllowed = label("allowed-2");
		when(metadataServiceMock.findLabelIds(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Set.of(allowed.getId(), alsoAllowed.getId(), "disallowed-1", "disallowed-2"));
		when(factoryMock.or()).thenReturn(orMock);
		when(orMock.hasClause()).thenReturn(true);
		when(orMock.toPredicate()).thenReturn(predicateMock);
		when(factoryMock.terms()).thenReturn(termsFieldStepMock);
		when(termsFieldStepMock.field(ErrandSearchPredicates.ACCESS_LABEL_ID_FIELD)).thenReturn(termsFieldMoreStepMock);
		when(termsFieldMoreStepMock.matchingAny(any(Set.class))).thenReturn(termsOptionsMock);
		when(factoryMock.not(termsOptionsMock)).thenReturn(notMock);

		assertThat(predicates().access(factoryMock, new AccessScope(true, Set.of(allowed, alsoAllowed), null), NAMESPACE, MUNICIPALITY_ID)).isSameAs(predicateMock);

		verify(termsFieldMoreStepMock).matchingAny(Set.of("disallowed-1", "disallowed-2"));
		verify(orMock).add(notMock);
		verify(factoryMock, never()).matchNone();
		verify(factoryMock, never()).match();
	}

	@Test
	void accessThroughAllLabelsOfTheNamespaceExcludesNothing() {
		final var allowed = label("allowed-1");
		when(metadataServiceMock.findLabelIds(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Set.of(allowed.getId()));
		when(factoryMock.or()).thenReturn(orMock);
		when(orMock.hasClause()).thenReturn(true);
		when(orMock.toPredicate()).thenReturn(predicateMock);
		when(factoryMock.matchAll()).thenReturn(matchAllMock);

		predicates().access(factoryMock, new AccessScope(true, Set.of(allowed), null), NAMESPACE, MUNICIPALITY_ID);

		verify(orMock).add(matchAllMock);
		verify(factoryMock, never()).terms();
	}

	@Test
	void accessThroughNoLabelsAtAllReachesNothingThroughLabels() {
		when(factoryMock.or()).thenReturn(orMock);
		when(orMock.hasClause()).thenReturn(true);
		when(orMock.toPredicate()).thenReturn(predicateMock);
		when(factoryMock.matchNone()).thenReturn(matchNoneMock);

		predicates().access(factoryMock, new AccessScope(true, Set.of(), null), NAMESPACE, MUNICIPALITY_ID);

		verify(orMock).add(matchNoneMock);
		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void accessThroughReportingAndLabels() {
		final var allowed = label("allowed-1");
		when(metadataServiceMock.findLabelIds(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Set.of(allowed.getId()));
		when(factoryMock.or()).thenReturn(orMock);
		when(orMock.hasClause()).thenReturn(true);
		when(orMock.toPredicate()).thenReturn(predicateMock);
		when(factoryMock.matchAll()).thenReturn(matchAllMock);
		when(factoryMock.match()).thenReturn(matchFieldStepMock);
		when(matchFieldStepMock.field(ErrandSearchPredicates.REPORTER_USER_ID_FIELD)).thenReturn(matchFieldMoreStepMock);
		when(matchFieldMoreStepMock.matching("rep01ort")).thenReturn(matchOptionsMock);

		assertThat(predicates().access(factoryMock, new AccessScope(true, Set.of(allowed), "rep01ort"), NAMESPACE, MUNICIPALITY_ID)).isSameAs(predicateMock);

		verify(orMock).add(matchAllMock);
		verify(orMock).add(matchOptionsMock);
	}

	private static MetadataLabelEntity label(final String id) {
		return MetadataLabelEntity.create().withId(id);
	}
}

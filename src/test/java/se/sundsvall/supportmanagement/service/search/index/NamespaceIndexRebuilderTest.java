package se.sundsvall.supportmanagement.service.search.index;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.massindexing.MassIndexerFilteringTypeStep;
import org.hibernate.search.mapper.orm.massindexing.MassIndexerReindexParameterStep;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEntityFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NamespaceIndexRebuilderTest {

	private static final String NAMESPACE = "My_Namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final Instant NOW = Instant.parse("2026-11-26T01:00:00Z");

	@Mock
	private EntityManagerFactory entityManagerFactoryMock;

	@Mock
	private OpenSearchClient openSearchMock;

	@Mock
	private RestClient restClientMock;

	@Mock
	private DeleteByQueryTask deleteByQueryMock;

	@Mock
	private SearchMapping searchMappingMock;

	@Mock
	private SearchScope<ErrandEntity> searchScopeMock;

	@Mock
	private MassIndexer massIndexerMock;

	@Mock
	private MassIndexerFilteringTypeStep filteringTypeStepMock;

	@Mock
	private MassIndexerReindexParameterStep reindexParameterStepMock;

	@Captor
	private ArgumentCaptor<MassIndexingMonitor> monitorCaptor;

	@Captor
	private ArgumentCaptor<MassIndexingFailureHandler> failureHandlerCaptor;

	@Captor
	private ArgumentCaptor<JsonNode> queryCaptor;

	private final List<String> reported = new ArrayList<>();

	private final NamespaceIndexRebuilder.Progress progress = new NamespaceIndexRebuilder.Progress() {

		@Override
		public void written(final long errands) {
			reported.add("written " + errands);
		}

		@Override
		public void removed(final long documents) {
			reported.add("removed " + documents);
		}
	};

	private NamespaceIndexRebuilder rebuilder() {
		return new NamespaceIndexRebuilder(entityManagerFactoryMock, openSearchMock, deleteByQueryMock, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private void massIndexer() {
		when(massIndexerMock.purgeAllOnStart(false)).thenReturn(massIndexerMock);
		when(massIndexerMock.monitor(monitorCaptor.capture())).thenReturn(massIndexerMock);
		when(massIndexerMock.failureHandler(failureHandlerCaptor.capture())).thenReturn(massIndexerMock);
		when(massIndexerMock.type(ErrandEntity.class)).thenReturn(filteringTypeStepMock);
		when(filteringTypeStepMock.reindexOnly("e.namespace = :namespace and e.municipalityId = :municipalityId")).thenReturn(reindexParameterStepMock);
		when(reindexParameterStepMock.param("namespace", NAMESPACE)).thenReturn(reindexParameterStepMock);
		when(reindexParameterStepMock.param("municipalityId", MUNICIPALITY_ID)).thenReturn(reindexParameterStepMock);
	}

	private NamespaceIndexRebuilder.Outcome rebuild() throws InterruptedException {
		massIndexer();
		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);
			when(searchMappingMock.scope(ErrandEntity.class)).thenAnswer(_ -> searchScopeMock);
			when(searchScopeMock.massIndexer()).thenReturn(massIndexerMock);

			return rebuilder().rebuild(NAMESPACE, MUNICIPALITY_ID, progress);
		}
	}

	private static MassIndexingEntityFailureContext failureOf(final Object id) {
		final var reference = mock(EntityReference.class);
		lenient().when(reference.id()).thenReturn(id);
		final var builder = MassIndexingEntityFailureContext.builder();
		builder.throwable(new IllegalArgumentException("mapper_parsing_exception"));
		builder.failingOperation("Indexing");
		builder.failingEntityReference(reference);
		return builder.build();
	}

	@Test
	void writesTheNamespaceThenRemovesWhatWasNotWritten() throws Exception {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		when(openSearchMock.restClient()).thenReturn(restClientMock);
		doAnswer(_ -> {
			monitorCaptor.getValue().documentsAdded(2);
			monitorCaptor.getValue().documentsAdded(3);
			return null;
		}).when(massIndexerMock).startAndWait();
		when(deleteByQueryMock.run(queryCaptor.capture(), any())).thenAnswer(invocation -> {
			invocation.<DeleteByQueryTask.ProgressListener>getArgument(1).removed(1);
			return 1L;
		});

		final var outcome = rebuild();

		assertThat(outcome).isEqualTo(new NamespaceIndexRebuilder.Outcome(5, 1));
		assertThat(reported).containsExactly("written 2", "written 5", "removed 1");

		final var refresh = ArgumentCaptor.forClass(Request.class);
		verify(restClientMock).performRequest(refresh.capture());
		assertThat(refresh.getValue().getMethod()).isEqualTo("POST");
		assertThat(refresh.getValue().getEndpoint()).isEqualTo("/errand-write/_refresh");

		// What was written somewhat before the rebuild started counts as written by it
		assertThat(queryCaptor.getValue()).isEqualTo(NamespaceIndexRebuilder.notWrittenSince(NAMESPACE, MUNICIPALITY_ID,
			NOW.minus(NamespaceIndexRebuilder.CLOCK_MARGIN).toEpochMilli()));
	}

	@Test
	void anErrandThatCouldNotBeWrittenLeavesTheIndexAsItIs() throws Exception {
		doAnswer(_ -> {
			failureHandlerCaptor.getValue().handle(failureOf("errand-1"));
			failureHandlerCaptor.getValue().handle(failureOf("errand-2"));
			throw new IllegalStateException("2 entities could not be indexed");
		}).when(massIndexerMock).startAndWait();

		final var e = assertThrows(IllegalStateException.class, this::rebuild);

		assertThat(e.getMessage()).isEqualTo(NamespaceIndexRebuilder.NOT_ALL_WRITTEN.formatted(2, "errand-1, errand-2"));
		verifyNoInteractions(deleteByQueryMock, restClientMock);
	}

	@Test
	void namesNoMoreThanSoManyErrands() throws Exception {
		doAnswer(_ -> {
			for (var i = 0; i < NamespaceIndexRebuilder.MAX_FAILURES_NAMED + 2; i++) {
				failureHandlerCaptor.getValue().handle(failureOf("errand-" + i));
			}
			return null;
		}).when(massIndexerMock).startAndWait();

		final var e = assertThrows(IllegalStateException.class, this::rebuild);

		assertThat(e.getMessage()).startsWith("12 errand(s) of the namespace could not be written").endsWith("errand-9 and 2 more");
		verifyNoInteractions(deleteByQueryMock);
	}

	@Test
	void aMassIndexerThatFailsByItselfFailsTheRebuild() throws Exception {
		doThrow(new IllegalStateException("cluster went away")).when(massIndexerMock).startAndWait();

		final var e = assertThrows(IllegalStateException.class, this::rebuild);

		assertThat(e.getMessage()).isEqualTo(NamespaceIndexRebuilder.FAILED.formatted("cluster went away"));
		verifyNoInteractions(deleteByQueryMock);
	}

	@Test
	void aRefreshThatFailsFailsTheRebuildBeforeAnythingIsRemoved() throws Exception {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		when(openSearchMock.restClient()).thenReturn(restClientMock);
		when(restClientMock.performRequest(any())).thenThrow(new IOException("gone"));

		assertThrows(RuntimeException.class, this::rebuild);

		verifyNoInteractions(deleteByQueryMock);
	}

	@Test
	void theDocumentsNotWrittenAreThoseOfTheNamespaceOlderThanTheMomentOrWithoutOne() {
		final var query = NamespaceIndexRebuilder.notWrittenSince(NAMESPACE, MUNICIPALITY_ID, 1000L);

		assertThat(query).hasToString(JsonMapper.builder().build().readTree("""
			{"bool": {
				"filter": [{"term": {"municipalityId": "2281"}}, {"term": {"namespace": "my_namespace"}}],
				"must_not": [{"range": {"indexedAt": {"gte": 1000, "format": "epoch_millis"}}}]
			}}""").toString());
	}

	@Test
	void aFailureNamingNoErrandIsToldByWhatWentWrong() {
		final var failures = new NamespaceIndexRebuilder.Failures();
		final var generic = MassIndexingFailureContext.builder();
		generic.throwable(new IllegalStateException("cluster went away"));
		generic.failingOperation("Fetching identifiers");
		final var withoutReference = MassIndexingEntityFailureContext.builder();
		withoutReference.throwable(new IllegalStateException("no reference"));
		withoutReference.failingOperation("Indexing");

		failures.handle(generic.build());
		failures.handle(withoutReference.build());

		assertThat(failures.isEmpty()).isFalse();
		assertThat(failures.describe()).isEqualTo(NamespaceIndexRebuilder.NOT_ALL_WRITTEN.formatted(2, "cluster went away, no reference"));
	}

	@Test
	void noFailureIsNothing() {
		assertThat(new NamespaceIndexRebuilder.Failures().isEmpty()).isTrue();
	}
}

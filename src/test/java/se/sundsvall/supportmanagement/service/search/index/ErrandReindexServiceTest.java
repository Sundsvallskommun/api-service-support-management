package se.sundsvall.supportmanagement.service.search.index;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.massindexing.MassIndexerFilteringTypeStep;
import org.hibernate.search.mapper.orm.massindexing.MassIndexerReindexParameterStep;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.config.SearchProperties;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@ExtendWith(MockitoExtension.class)
class ErrandReindexServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final Duration LOCK_AT_MOST_FOR = Duration.ofHours(6);

	@Mock
	private EntityManagerFactory entityManagerFactoryMock;

	@Mock
	private LockProvider lockProviderMock;

	@Mock
	private SimpleLock lockMock;

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

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private OpenSearchClient openSearchMock;

	@Mock
	private RestClient restClientMock;

	private ErrandReindexService service(final boolean enabled) {
		return new ErrandReindexService(entityManagerFactoryMock, openSearchMock, lockProviderMock, new SearchAvailability(enabled),
			new SearchProperties(10000, new SearchProperties.Reindex(LOCK_AT_MOST_FOR)), accessControlServiceMock);
	}

	private void purgeAnswers() throws IOException {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		when(openSearchMock.restClient()).thenReturn(restClientMock);
		when(restClientMock.performRequest(any())).thenReturn(null);
	}

	@Test
	void reindexWhenDisabled() {
		final var e = assertThrows(ThrowableProblem.class, () -> service(false).reindex(NAMESPACE, MUNICIPALITY_ID, false));

		assertThat(e.getStatus()).isEqualTo(SERVICE_UNAVAILABLE);
		verifyNoInteractions(lockProviderMock, accessControlServiceMock);
	}

	@Test
	void reindexWhenNotAuthorized() {
		doThrow(Problem.valueOf(FORBIDDEN, "no")).when(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, RW);

		final var e = assertThrows(ThrowableProblem.class, () -> service(true).reindex(NAMESPACE, MUNICIPALITY_ID, true));

		assertThat(e.getStatus()).isEqualTo(FORBIDDEN);
		verifyNoInteractions(lockProviderMock);
	}

	@Test
	void reindexWhileAnotherIsRunning() {
		when(lockProviderMock.lock(any())).thenReturn(Optional.empty());

		final var e = assertThrows(ThrowableProblem.class, () -> service(true).reindex(NAMESPACE, MUNICIPALITY_ID, false));

		assertThat(e.getStatus()).isEqualTo(CONFLICT);
		assertThat(e.getDetail()).isEqualTo(ErrandReindexService.REINDEX_RUNNING);
		verify(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, RW);

		final var configuration = ArgumentCaptor.forClass(LockConfiguration.class);
		verify(lockProviderMock).lock(configuration.capture());
		assertThat(configuration.getValue().getName()).isEqualTo(ErrandReindexService.LOCK_NAME);
		assertThat(configuration.getValue().getLockAtMostFor()).isEqualTo(LOCK_AT_MOST_FOR);
	}

	@Test
	void reindexNamespace() throws IOException {
		final var indexing = new CompletableFuture<Void>();
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		purgeAnswers();
		when(massIndexerMock.type(ErrandEntity.class)).thenReturn(filteringTypeStepMock);
		when(filteringTypeStepMock.reindexOnly("e.namespace = :namespace and e.municipalityId = :municipalityId")).thenReturn(reindexParameterStepMock);
		when(reindexParameterStepMock.param("namespace", NAMESPACE)).thenReturn(reindexParameterStepMock);
		when(reindexParameterStepMock.param("municipalityId", MUNICIPALITY_ID)).thenReturn(reindexParameterStepMock);
		when(massIndexerMock.purgeAllOnStart(false)).thenReturn(massIndexerMock);
		when(massIndexerMock.start()).thenAnswer(_ -> indexing);

		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);
			when(searchMappingMock.scope(ErrandEntity.class)).thenAnswer(_ -> searchScopeMock);
			when(searchScopeMock.massIndexer()).thenReturn(massIndexerMock);

			service(true).reindex(NAMESPACE, MUNICIPALITY_ID, false);
		}

		// The documents of the namespace, and of that namespace alone, are removed before the rebuild
		final var request = ArgumentCaptor.forClass(Request.class);
		verify(restClientMock).performRequest(request.capture());
		assertThat(request.getValue().getMethod()).isEqualTo("POST");
		assertThat(request.getValue().getEndpoint()).isEqualTo("/errand-write/_delete_by_query");
		assertThat(request.getValue().getParameters()).containsEntry("conflicts", "proceed").containsEntry("refresh", "true");
		assertThat(EntityUtils.toString(request.getValue().getEntity()))
			.isEqualTo("{\"query\": {\"bool\": {\"filter\": [{\"term\": {\"municipalityId\": \"2281\"}}, {\"term\": {\"namespace\": \"namespace\"}}]}}}");

		// The lock is held until the indexing, which goes on in the background, is over
		verify(massIndexerMock, never()).dropAndCreateSchemaOnStart(true);
		verify(lockMock, never()).unlock();
		indexing.complete(null);
		verify(lockMock).unlock();
	}

	@Test
	void reindexNamespaceReleasesTheLockWhenThePurgeFails() throws IOException {
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		purgeAnswers();
		when(restClientMock.performRequest(any())).thenThrow(new IOException("gone"));

		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);

			final var service = service(true);
			assertThrows(UncheckedIOException.class, () -> service.reindex(NAMESPACE, MUNICIPALITY_ID, false));
		}

		verify(lockMock).unlock();
		verify(massIndexerMock, never()).start();
	}

	@Test
	void reindexEverything() {
		final var indexing = new CompletableFuture<Void>();
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		when(massIndexerMock.dropAndCreateSchemaOnStart(true)).thenReturn(massIndexerMock);
		when(massIndexerMock.start()).thenAnswer(_ -> indexing);

		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);
			when(searchMappingMock.scope(ErrandEntity.class)).thenAnswer(_ -> searchScopeMock);
			when(searchScopeMock.massIndexer()).thenReturn(massIndexerMock);

			service(true).reindex(NAMESPACE, MUNICIPALITY_ID, true);
		}

		verify(massIndexerMock, never()).purgeAllOnStart(false);
		verify(massIndexerMock, never()).type(any());
		verifyNoInteractions(restClientMock);
		indexing.completeExceptionally(new IllegalStateException("cluster gone"));
		verify(lockMock).unlock();
	}

	@Test
	void reindexReleasesTheLockWhenStartingFails() {
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		when(openSearchMock.errandWriteIndex()).thenThrow(new IllegalStateException("no search mapping"));

		final var service = service(true);
		assertThrows(IllegalStateException.class, () -> service.reindex(NAMESPACE, MUNICIPALITY_ID, false));

		verify(lockMock).unlock();
	}
}

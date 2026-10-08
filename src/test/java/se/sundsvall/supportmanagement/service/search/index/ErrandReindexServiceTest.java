package se.sundsvall.supportmanagement.service.search.index;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
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
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

	private ErrandReindexService service(final boolean enabled) {
		return new ErrandReindexService(entityManagerFactoryMock, openSearchMock, lockProviderMock, new SearchAvailability(enabled),
			new SearchProperties(10000, Duration.ofSeconds(10), 100, new SearchProperties.Reindex(LOCK_AT_MOST_FOR)), accessControlServiceMock, namespaceConfigRepositoryMock);
	}

	private void purgeAnswers() throws IOException {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		when(openSearchMock.restClient()).thenReturn(restClientMock);
		when(restClientMock.performRequest(any())).thenReturn(null);
	}

	@Test
	void reindexWhenDisabled() {
		final var e = assertThrows(ThrowableProblem.class, () -> service(false).reindex(NAMESPACE, MUNICIPALITY_ID));

		assertThat(e.getStatus()).isEqualTo(SERVICE_UNAVAILABLE);
		verifyNoInteractions(lockProviderMock, accessControlServiceMock);
	}

	@Test
	void reindexWhenNotAuthorized() {
		doThrow(Problem.valueOf(FORBIDDEN, "no")).when(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, RW);

		final var service = service(true);
		final var e = assertThrows(ThrowableProblem.class, () -> service.reindex(NAMESPACE, MUNICIPALITY_ID));

		assertThat(e.getStatus()).isEqualTo(FORBIDDEN);
		verifyNoInteractions(lockProviderMock);
	}

	/**
	 * The whole index is every namespace at once, so it asks that the caller may administer every namespace that enforces
	 * access control. One that is not theirs refuses the rebuild of all of them.
	 */
	@Test
	void reindexingEverythingAsksEveryNamespaceThatEnforcesAnything() {
		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(
			NamespaceConfigEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID),
			NamespaceConfigEntity.create().withNamespace("other").withMunicipalityId("2282")));
		doNothing().when(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, RW);
		doThrow(Problem.valueOf(FORBIDDEN, "no")).when(accessControlServiceMock)
			.verifyNamespaceAuthorization("other", "2282", ProtectedResource.NAMESPACE_CONFIG, RW);

		final var service = service(true);
		final var e = assertThrows(ThrowableProblem.class, service::reindexEverything);

		assertThat(e.getStatus()).isEqualTo(FORBIDDEN);
		verify(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, RW);
		verifyNoInteractions(lockProviderMock);
	}

	@Test
	void reindexWhileAnotherIsRunning() {
		when(lockProviderMock.lock(any())).thenReturn(Optional.empty());

		final var e = assertThrows(ThrowableProblem.class, () -> service(true).reindex(NAMESPACE, MUNICIPALITY_ID));

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

			service(true).reindex(NAMESPACE, MUNICIPALITY_ID);
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

	/**
	 * The namespace is indexed lowercased, as the database compares it, and this term goes to OpenSearch without passing
	 * the query DSL: purging one casing while the documents carry another left them behind for the rebuild to duplicate.
	 */
	@Test
	void reindexNamespacePurgesWhateverCasingItWasAskedFor() throws IOException {
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		purgeAnswers();
		when(massIndexerMock.type(ErrandEntity.class)).thenReturn(filteringTypeStepMock);
		when(filteringTypeStepMock.reindexOnly("e.namespace = :namespace and e.municipalityId = :municipalityId")).thenReturn(reindexParameterStepMock);
		when(reindexParameterStepMock.param("namespace", "My_Namespace")).thenReturn(reindexParameterStepMock);
		when(reindexParameterStepMock.param("municipalityId", MUNICIPALITY_ID)).thenReturn(reindexParameterStepMock);
		when(massIndexerMock.purgeAllOnStart(false)).thenReturn(massIndexerMock);
		when(massIndexerMock.start()).thenAnswer(_ -> new CompletableFuture<Void>());

		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);
			when(searchMappingMock.scope(ErrandEntity.class)).thenAnswer(_ -> searchScopeMock);
			when(searchScopeMock.massIndexer()).thenReturn(massIndexerMock);

			service(true).reindex("My_Namespace", MUNICIPALITY_ID);
		}

		final var request = ArgumentCaptor.forClass(Request.class);
		verify(restClientMock).performRequest(request.capture());
		assertThat(EntityUtils.toString(request.getValue().getEntity())).contains("\"namespace\": \"my_namespace\"");
	}

	@Test
	void reindexNamespaceReleasesTheLockWhenThePurgeFails() throws IOException {
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		purgeAnswers();
		when(restClientMock.performRequest(any())).thenThrow(new IOException("gone"));

		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);

			final var service = service(true);
			assertThrows(UncheckedIOException.class, () -> service.reindex(NAMESPACE, MUNICIPALITY_ID));
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

			service(true).reindexEverything();
		}

		verify(massIndexerMock, never()).purgeAllOnStart(false);
		verify(massIndexerMock, never()).type(any());
		verifyNoInteractions(restClientMock);
		indexing.completeExceptionally(new IllegalStateException("cluster gone"));
		verify(lockMock).unlock();
	}

	@Test
	void theNightlyReindexDoesNothingWhileSearchIsOff() {
		service(false).reindexEveryNamespace();

		verifyNoInteractions(lockProviderMock, namespaceConfigRepositoryMock, entityManagerFactoryMock, openSearchMock);
	}

	@Test
	void theNightlyReindexStandsAsideForOneAlreadyRunning() {
		when(lockProviderMock.lock(any())).thenReturn(Optional.empty());

		service(true).reindexEveryNamespace();

		verifyNoInteractions(namespaceConfigRepositoryMock, entityManagerFactoryMock, openSearchMock);
	}

	@Test
	void theNightlyReindexWritesEveryNamespaceOverAndWaitsForIt() throws Exception {
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(
			NamespaceConfigEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID),
			NamespaceConfigEntity.create().withNamespace("other").withMunicipalityId("2282")));
		purgeAnswers();
		when(massIndexerMock.type(any())).thenReturn(filteringTypeStepMock);
		when(filteringTypeStepMock.reindexOnly(any())).thenReturn(reindexParameterStepMock);
		when(reindexParameterStepMock.param(any(), any())).thenReturn(reindexParameterStepMock);
		when(massIndexerMock.purgeAllOnStart(false)).thenReturn(massIndexerMock);

		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);
			when(searchMappingMock.scope(ErrandEntity.class)).thenAnswer(_ -> searchScopeMock);
			when(searchScopeMock.massIndexer()).thenReturn(massIndexerMock);

			service(true).reindexEveryNamespace();
		}

		// A namespace is emptied and written again before the next one is taken, so only one is searched with a hole in it
		verify(restClientMock, times(2)).performRequest(any());
		verify(massIndexerMock, times(2)).startAndWait();
		verify(massIndexerMock, never()).start();
		verify(massIndexerMock, never()).dropAndCreateSchemaOnStart(true);
		verify(lockMock).unlock();
	}

	/**
	 * A namespace the index will not take must not leave the namespaces after it unrepaired, so the walk goes on and what
	 * failed is raised at the end, where the scheduled job reports it.
	 */
	@Test
	void theNightlyReindexCarriesOnPastANamespaceThatFails() throws Exception {
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(
			NamespaceConfigEntity.create().withNamespace("first").withMunicipalityId(MUNICIPALITY_ID),
			NamespaceConfigEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID)));
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		when(openSearchMock.restClient()).thenReturn(restClientMock);
		// The first namespace is refused by the index, the second is rebuilt
		when(restClientMock.performRequest(any())).thenThrow(new IOException("cluster said no")).thenReturn(null);
		when(massIndexerMock.type(any())).thenReturn(filteringTypeStepMock);
		when(filteringTypeStepMock.reindexOnly(any())).thenReturn(reindexParameterStepMock);
		when(reindexParameterStepMock.param(any(), any())).thenReturn(reindexParameterStepMock);
		when(massIndexerMock.purgeAllOnStart(false)).thenReturn(massIndexerMock);

		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);
			when(searchMappingMock.scope(ErrandEntity.class)).thenAnswer(_ -> searchScopeMock);
			when(searchScopeMock.massIndexer()).thenReturn(massIndexerMock);

			final var service = service(true);
			final var e = assertThrows(IllegalStateException.class, service::reindexEveryNamespace);

			assertThat(e).hasMessageContaining("rebuilt 1 of 2").hasMessageContaining("%s/first".formatted(MUNICIPALITY_ID));
		}

		// The second namespace was rebuilt all the same
		verify(massIndexerMock).startAndWait();
		verify(lockMock).unlock();
	}

	@Test
	void theNightlyReindexReleasesTheLockWhenANamespaceFails() throws Exception {
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(NamespaceConfigEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID)));
		when(openSearchMock.errandWriteIndex()).thenThrow(new IllegalStateException("no search mapping"));

		final var service = service(true);
		assertThrows(IllegalStateException.class, service::reindexEveryNamespace);

		verify(lockMock).unlock();
	}

	@Test
	void reindexReleasesTheLockWhenStartingFails() {
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		when(openSearchMock.errandWriteIndex()).thenThrow(new IllegalStateException("no search mapping"));

		final var service = service(true);
		assertThrows(IllegalStateException.class, () -> service.reindex(NAMESPACE, MUNICIPALITY_ID));

		verify(lockMock).unlock();
	}
}

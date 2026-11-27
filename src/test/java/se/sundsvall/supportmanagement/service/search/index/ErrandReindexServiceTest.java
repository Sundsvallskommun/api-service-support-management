package se.sundsvall.supportmanagement.service.search.index;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.config.JobProperties;
import se.sundsvall.supportmanagement.config.SearchProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.model.enums.JobStatus;
import se.sundsvall.supportmanagement.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.job.JobService;
import se.sundsvall.supportmanagement.service.job.JobSpec;
import se.sundsvall.supportmanagement.service.job.SearchReindexRun;
import se.sundsvall.supportmanagement.service.job.SearchReindexRunner;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import static se.sundsvall.supportmanagement.model.enums.JobStatus.COMPLETED;
import static se.sundsvall.supportmanagement.model.enums.JobStatus.FAILED;
import static se.sundsvall.supportmanagement.model.enums.JobType.SEARCH_REINDEX;

@ExtendWith(MockitoExtension.class)
class ErrandReindexServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String JOB_ID = "job-id";
	private static final Duration LOCK_AT_MOST_FOR = Duration.ofHours(6);
	private static final Duration STALE_AFTER = Duration.ofMinutes(30);

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
	private SearchSchemaManager schemaManagerMock;

	@Mock
	private MassIndexer massIndexerMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private OpenSearchClient openSearchMock;

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private JobService jobServiceMock;

	@Mock
	private SearchReindexRunner runnerMock;

	@Mock
	private AsyncTaskExecutor taskExecutorMock;

	@Mock
	private SearchIndexHealth indexHealthMock;

	@Captor
	private ArgumentCaptor<JobSpec> specCaptor;

	@Captor
	private ArgumentCaptor<Function<String, SearchReindexRun>> toRunCaptor;

	@Captor
	private ArgumentCaptor<Consumer<SearchReindexRun>> carryOutCaptor;

	private ErrandReindexService service(final boolean enabled) {
		return new ErrandReindexService(entityManagerFactoryMock, lockProviderMock, new SearchAvailability(enabled),
			new SearchProperties(10000, Duration.ofSeconds(10), 100, new SearchProperties.Reindex(LOCK_AT_MOST_FOR, Duration.ofMinutes(10), Duration.ofSeconds(2))),
			accessControlServiceMock, namespaceConfigRepositoryMock, errandsRepositoryMock, jobServiceMock, runnerMock, taskExecutorMock, indexHealthMock, openSearchMock,
			new JobProperties(STALE_AFTER, Duration.ofDays(90)));
	}

	private static JobResponse job(final JobStatus status, final String message) {
		return JobResponse.create().withJobId(JOB_ID).withType(SEARCH_REINDEX).withStatus(status).withMessage(message);
	}

	@Test
	void reindexWhenDisabled() {
		final var e = assertThrows(ThrowableProblem.class, () -> service(false).reindex(NAMESPACE, MUNICIPALITY_ID));

		assertThat(e.getStatus()).isEqualTo(SERVICE_UNAVAILABLE);
		verifyNoInteractions(lockProviderMock, accessControlServiceMock, jobServiceMock);
	}

	@Test
	void reindexWhenNotAuthorized() {
		doThrow(Problem.valueOf(FORBIDDEN, "no")).when(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, RW);

		final var service = service(true);
		final var e = assertThrows(ThrowableProblem.class, () -> service.reindex(NAMESPACE, MUNICIPALITY_ID));

		assertThat(e.getStatus()).isEqualTo(FORBIDDEN);
		verifyNoInteractions(lockProviderMock, jobServiceMock);
	}

	@Test
	void reindexWhileAnotherIsRunning() {
		when(lockProviderMock.lock(any())).thenReturn(Optional.empty());

		final var service = service(true);
		final var e = assertThrows(ThrowableProblem.class, () -> service.reindex(NAMESPACE, MUNICIPALITY_ID));

		assertThat(e.getStatus()).isEqualTo(CONFLICT);
		final var configuration = ArgumentCaptor.forClass(LockConfiguration.class);
		verify(lockProviderMock).lock(configuration.capture());
		assertThat(configuration.getValue().getName()).isEqualTo(ErrandReindexService.LOCK_NAME);
		assertThat(configuration.getValue().getLockAtMostFor()).isEqualTo(LOCK_AT_MOST_FOR);
		verifyNoInteractions(jobServiceMock);
	}

	@Test
	void reindexWhileAJobOfTheNamespaceIsStillAlive() {
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		when(jobServiceMock.stealStaleLease(NAMESPACE, MUNICIPALITY_ID, SEARCH_REINDEX, STALE_AFTER)).thenReturn(false);

		final var service = service(true);
		final var e = assertThrows(ThrowableProblem.class, () -> service.reindex(NAMESPACE, MUNICIPALITY_ID));

		assertThat(e.getStatus()).isEqualTo(CONFLICT);
		verify(lockMock).unlock();
		verify(jobServiceMock, never()).launch(any(), any(), any(), any(), any());
	}

	@Test
	void reindexStartsAJobThatReleasesTheLockWhenItEnds() {
		final var started = job(JobStatus.PENDING, null);
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		when(jobServiceMock.stealStaleLease(NAMESPACE, MUNICIPALITY_ID, SEARCH_REINDEX, STALE_AFTER)).thenReturn(true);
		when(errandsRepositoryMock.count(any(Specification.class))).thenReturn(42L);
		when(jobServiceMock.launch(specCaptor.capture(), eq(taskExecutorMock), toRunCaptor.capture(), carryOutCaptor.capture(), eq(ErrandReindexService.COULD_NOT_START)))
			.thenReturn(started);

		final var result = service(true).reindex(NAMESPACE, MUNICIPALITY_ID);

		assertThat(result).isSameAs(started);
		assertThat(specCaptor.getValue()).isEqualTo(new JobSpec(NAMESPACE, MUNICIPALITY_ID, SEARCH_REINDEX, 42, null));
		final var run = toRunCaptor.getValue().apply(JOB_ID);
		assertThat(run).isEqualTo(new SearchReindexRun(JOB_ID, NAMESPACE, MUNICIPALITY_ID));

		// The lock is held until the run has ended, and released once however often that is said
		verify(lockMock, never()).unlock();
		carryOutCaptor.getValue().accept(run);
		carryOutCaptor.getValue().accept(run);
		verify(runnerMock, times(2)).run(run);
		verify(lockMock).unlock();
	}

	@Test
	void reindexReleasesTheLockWhenTheJobCouldNotBeStarted() {
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		when(jobServiceMock.stealStaleLease(NAMESPACE, MUNICIPALITY_ID, SEARCH_REINDEX, STALE_AFTER)).thenReturn(true);
		when(jobServiceMock.launch(any(), any(), any(), any(), any())).thenThrow(new TaskRejectedException("busy"));

		final var service = service(true);
		assertThrows(TaskRejectedException.class, () -> service.reindex(NAMESPACE, MUNICIPALITY_ID));

		verify(lockMock).unlock();
	}

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
		verifyNoInteractions(lockProviderMock);
	}

	@Test
	void reindexEverythingWritesTheLayoutWhenDone() {
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
		verify(lockMock, never()).unlock();
		verifyNoInteractions(indexHealthMock, jobServiceMock);

		indexing.complete(null);

		verify(indexHealthMock).created();
		verify(lockMock).unlock();
	}

	@Test
	void reindexEverythingThatFailsLeavesTheLayoutAlone() {
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

		indexing.completeExceptionally(new IllegalStateException("gone"));

		verifyNoInteractions(indexHealthMock);
		verify(lockMock).unlock();
	}

	@Test
	void theNightlyReindexDoesNothingWhileSearchIsOff() {
		service(false).reindexEveryNamespace();

		verifyNoInteractions(lockProviderMock, namespaceConfigRepositoryMock, entityManagerFactoryMock, openSearchMock, jobServiceMock);
	}

	@Test
	void theNightlyReindexStandsAsideForOneAlreadyRunning() {
		when(lockProviderMock.lock(any())).thenReturn(Optional.empty());

		service(true).reindexEveryNamespace();

		verifyNoInteractions(namespaceConfigRepositoryMock, entityManagerFactoryMock, openSearchMock, jobServiceMock);
	}

	@Test
	void theNightlyReindexCreatesAMissingIndexAndRunsAJobPerNamespaceOnItsOwnThread() throws IOException {
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		when(openSearchMock.errandIndexExists()).thenReturn(false);
		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(
			NamespaceConfigEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID),
			NamespaceConfigEntity.create().withNamespace("other").withMunicipalityId("2282")));
		when(jobServiceMock.stealStaleLease(anyString(), anyString(), eq(SEARCH_REINDEX), eq(STALE_AFTER))).thenReturn(true);
		final var executors = ArgumentCaptor.forClass(AsyncTaskExecutor.class);
		when(jobServiceMock.launch(any(), executors.capture(), any(), any(), any())).thenReturn(job(COMPLETED, "done"));

		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);
			when(searchMappingMock.scope(ErrandEntity.class)).thenAnswer(_ -> searchScopeMock);
			when(searchScopeMock.schemaManager()).thenReturn(schemaManagerMock);

			service(true).reindexEveryNamespace();
		}

		verify(schemaManagerMock).createIfMissing();
		verify(indexHealthMock).created();
		verify(jobServiceMock, times(2)).launch(specCaptor.capture(), any(), any(), any(), any());
		assertThat(specCaptor.getAllValues()).extracting(JobSpec::namespace).containsExactly(NAMESPACE, "other");

		// Carried out on the calling thread, so that one namespace is done before the next is started
		final var ranOn = new Thread[1];
		executors.getValue().execute(() -> ranOn[0] = Thread.currentThread());
		assertThat(ranOn[0]).isSameAs(Thread.currentThread());
		assertThat(executors.getAllValues()).doesNotContain(taskExecutorMock);
		verify(lockMock).unlock();
	}

	@Test
	void theNightlyReindexLeavesAnIndexThatIsThereAlone() throws IOException {
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		when(openSearchMock.errandIndexExists()).thenReturn(true);
		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of());

		service(true).reindexEveryNamespace();

		verifyNoInteractions(entityManagerFactoryMock, indexHealthMock);
		verify(lockMock).unlock();
	}

	@Test
	void theNightlyReindexGoesOnPastANamespaceThatFailsAndReportsItAtTheEnd() throws IOException {
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
		when(openSearchMock.errandIndexExists()).thenReturn(true);
		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(
			NamespaceConfigEntity.create().withNamespace("first").withMunicipalityId(MUNICIPALITY_ID),
			NamespaceConfigEntity.create().withNamespace("second").withMunicipalityId(MUNICIPALITY_ID),
			NamespaceConfigEntity.create().withNamespace("third").withMunicipalityId(MUNICIPALITY_ID)));
		when(jobServiceMock.stealStaleLease(anyString(), anyString(), eq(SEARCH_REINDEX), eq(STALE_AFTER))).thenReturn(true);
		when(jobServiceMock.launch(any(), any(), any(), any(), any()))
			.thenReturn(job(FAILED, "errands could not be written"))
			.thenThrow(Problem.valueOf(CONFLICT, "already running"))
			.thenReturn(job(COMPLETED, "done"));

		final var service = service(true);
		final var e = assertThrows(IllegalStateException.class, service::reindexEveryNamespace);

		assertThat(e.getMessage())
			.startsWith("Nightly reindex rebuilt 1 of 3 namespaces, these failed: ")
			.contains("2281/first (ended as FAILED: errands could not be written)")
			.contains("2281/second (")
			.doesNotContain("third");
		verify(jobServiceMock, times(3)).launch(any(), any(), any(), any(), any());
		verify(lockMock).unlock();
	}
}

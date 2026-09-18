package se.sundsvall.supportmanagement.service.search;

import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
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
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.config.SearchProperties;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
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

	private ErrandReindexService service(final boolean enabled) {
		return new ErrandReindexService(entityManagerFactoryMock, lockProviderMock, new SearchAvailability(enabled),
			new SearchProperties(10000, new SearchProperties.Reindex(LOCK_AT_MOST_FOR)));
	}

	@Test
	void reindexWhenDisabled() {
		final var e = assertThrows(ThrowableProblem.class, () -> service(false).reindex(NAMESPACE, MUNICIPALITY_ID, false));

		assertThat(e.getStatus()).isEqualTo(SERVICE_UNAVAILABLE);
		verifyNoInteractions(lockProviderMock);
	}

	@Test
	void reindexWhileAnotherIsRunning() {
		when(lockProviderMock.lock(any())).thenReturn(Optional.empty());

		final var e = assertThrows(ThrowableProblem.class, () -> service(true).reindex(NAMESPACE, MUNICIPALITY_ID, false));

		assertThat(e.getStatus()).isEqualTo(CONFLICT);
		assertThat(e.getDetail()).isEqualTo(ErrandReindexService.REINDEX_RUNNING);

		final var configuration = ArgumentCaptor.forClass(LockConfiguration.class);
		verify(lockProviderMock).lock(configuration.capture());
		assertThat(configuration.getValue().getName()).isEqualTo(ErrandReindexService.LOCK_NAME);
		assertThat(configuration.getValue().getLockAtMostFor()).isEqualTo(LOCK_AT_MOST_FOR);
	}

	@Test
	void reindexNamespace() {
		final var indexing = new CompletableFuture<Void>();
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));
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

		// The lock is held until the indexing, which goes on in the background, is over
		verify(massIndexerMock, never()).dropAndCreateSchemaOnStart(true);
		verify(lockMock, never()).unlock();
		indexing.complete(null);
		verify(lockMock).unlock();
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
		indexing.completeExceptionally(new IllegalStateException("cluster gone"));
		verify(lockMock).unlock();
	}

	@Test
	void reindexReleasesTheLockWhenStartingFails() {
		when(lockProviderMock.lock(any())).thenReturn(Optional.of(lockMock));

		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenThrow(new IllegalStateException("no search mapping"));

			final var service = service(true);
			assertThrows(IllegalStateException.class, () -> service.reindex(NAMESPACE, MUNICIPALITY_ID, false));
		}

		verify(lockMock).unlock();
	}
}

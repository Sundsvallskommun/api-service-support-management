package se.sundsvall.supportmanagement.service.scheduler.softdeletederrands;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;
import se.sundsvall.supportmanagement.service.ErrandService;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_NOTIFICATION_TTL_IN_DAYS;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_SOFT_DELETED_RETENTION_IN_DAYS;
import static se.sundsvall.supportmanagement.model.enums.ValueType.INTEGER;
import static se.sundsvall.supportmanagement.service.scheduler.softdeletederrands.SoftDeletedErrandWorker.BATCH_SIZE;

@ExtendWith(MockitoExtension.class)
class SoftDeletedErrandWorkerTest {

	private static final String MUNICIPALITY_ID = "2281";
	// The default the worker is configured with
	private static final int MAX_ERRANDS_PER_NAMESPACE = 500;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

	@Mock
	private ErrandService errandServiceMock;

	@Captor
	private ArgumentCaptor<OffsetDateTime> deletedBeforeCaptor;

	private SoftDeletedErrandWorker worker;

	@BeforeEach
	void setUp() {
		worker = workerWith(MAX_ERRANDS_PER_NAMESPACE);
	}

	private SoftDeletedErrandWorker workerWith(final int maxErrandsPerNamespace) {
		return new SoftDeletedErrandWorker(errandsRepositoryMock, namespaceConfigRepositoryMock, errandServiceMock, maxErrandsPerNamespace);
	}

	@Test
	void deleteSoftDeletedErrands() {
		final var firstBatch = IntStream.range(0, BATCH_SIZE).mapToObj("id-%03d"::formatted).toList();

		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(config("KEEPING", null), config("NAMESPACE", 30), config("SHORT", 1)));
		// Three batches for the namespace keeping its soft deleted errands for 30 days, the last of them empty, and two for
		// the one keeping them for a single day
		when(errandsRepositoryMock.findIdsAfter(any(), any(), anyInt()))
			.thenReturn(firstBatch, List.of("id-restored", "id-failing", "id-last"), emptyList(), List.of("id-short"), emptyList());
		when(errandServiceMock.deleteSoftDeletedErrand(any(), any())).thenReturn(true);
		when(errandServiceMock.deleteSoftDeletedErrand(eq("id-restored"), any())).thenReturn(false);
		when(errandServiceMock.deleteSoftDeletedErrand(eq("id-failing"), any())).thenThrow(new IllegalStateException("locked decision"));

		assertThat(worker.deleteSoftDeletedErrands()).isEmpty();

		verify(namespaceConfigRepositoryMock).findAll();
		// Each namespace is walked from its beginning, and each following batch starts after the id the previous one ended on
		verify(errandsRepositoryMock, times(2)).findIdsAfter(any(), isNull(), eq(BATCH_SIZE));
		verify(errandsRepositoryMock).findIdsAfter(any(), eq(firstBatch.getLast()), eq(BATCH_SIZE));
		verify(errandsRepositoryMock).findIdsAfter(any(), eq("id-last"), eq(BATCH_SIZE));
		verify(errandsRepositoryMock).findIdsAfter(any(), eq("id-short"), eq(BATCH_SIZE));
		// Every errand of the namespace keeping them for 30 days first, and the one of the namespace keeping them for a day
		// last
		verify(errandServiceMock, times(BATCH_SIZE + 4)).deleteSoftDeletedErrand(any(), deletedBeforeCaptor.capture());
		final var deletedBefores = deletedBeforeCaptor.getAllValues();
		assertThat(deletedBefores.subList(0, BATCH_SIZE + 3)).allSatisfy(deletedBefore -> assertThat(deletedBefore).isCloseTo(OffsetDateTime.now().minusDays(30), within(5, SECONDS)));
		assertThat(deletedBefores.getLast()).isCloseTo(OffsetDateTime.now().minusDays(1), within(5, SECONDS));
		firstBatch.forEach(id -> verify(errandServiceMock).deleteSoftDeletedErrand(eq(id), any()));
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-restored"), any());
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-failing"), any());
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-last"), any());
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-short"), any());
		verifyNoMoreInteractions(namespaceConfigRepositoryMock, errandsRepositoryMock, errandServiceMock);
	}

	@Test
	void deleteSoftDeletedErrandsWhenAnErrandFailsWithoutMessage() {
		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(config("NAMESPACE", 30)));
		when(errandsRepositoryMock.findIdsAfter(any(), any(), anyInt())).thenReturn(List.of("id-failing"), emptyList());
		when(errandServiceMock.deleteSoftDeletedErrand(eq("id-failing"), any())).thenThrow(new IllegalStateException());

		// An errand that fails leaves the walk of its namespace to carry on, so the namespace is not accounted as aborted
		assertThat(worker.deleteSoftDeletedErrands()).isEmpty();

		verify(namespaceConfigRepositoryMock).findAll();
		verify(errandsRepositoryMock).findIdsAfter(any(), isNull(), eq(BATCH_SIZE));
		verify(errandsRepositoryMock).findIdsAfter(any(), eq("id-failing"), eq(BATCH_SIZE));
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-failing"), any());
		verifyNoMoreInteractions(namespaceConfigRepositoryMock, errandsRepositoryMock, errandServiceMock);
	}

	@Test
	void deleteSoftDeletedErrandsUpToTheCapOfEachNamespace() {
		worker = workerWith(2);
		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(config("BACKLOG", 30), config("NAMESPACE", 30)));
		when(errandsRepositoryMock.findIdsAfter(any(), any(), anyInt()))
			.thenReturn(List.of("id-a", "id-failing", "id-c", "id-d"), List.of("id-e"), emptyList());
		when(errandServiceMock.deleteSoftDeletedErrand(any(), any())).thenReturn(true);
		when(errandServiceMock.deleteSoftDeletedErrand(eq("id-failing"), any())).thenThrow(new IllegalStateException("locked decision"));

		assertThat(worker.deleteSoftDeletedErrands()).isEmpty();

		verify(namespaceConfigRepositoryMock).findAll();
		// The errand that could not be deleted does not count towards the cap, so the walk of the namespace with a backlog
		// stops after two errands deleted, leaving the last one for the next run, and the namespace after it gets a cap of
		// its own
		verify(errandsRepositoryMock, times(2)).findIdsAfter(any(), isNull(), eq(BATCH_SIZE));
		verify(errandsRepositoryMock).findIdsAfter(any(), eq("id-e"), eq(BATCH_SIZE));
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-a"), any());
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-failing"), any());
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-c"), any());
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-e"), any());
		verifyNoMoreInteractions(namespaceConfigRepositoryMock, errandsRepositoryMock, errandServiceMock);
	}

	@Test
	void deleteSoftDeletedErrandsWhenNoNamespaceSetsARetention() {
		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(config("KEEPING", null)));

		assertThat(worker.deleteSoftDeletedErrands()).isEmpty();

		verify(namespaceConfigRepositoryMock).findAll();
		verifyNoMoreInteractions(namespaceConfigRepositoryMock);
		verifyNoInteractions(errandsRepositoryMock, errandServiceMock);
	}

	@Test
	void deleteSoftDeletedErrandsWhenANamespaceCannotBeWalked() {
		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(config("FAILING", 30), config("NAMESPACE", 30), config("BROKEN", 30)));
		when(errandsRepositoryMock.findIdsAfter(any(), any(), anyInt()))
			.thenThrow(new IllegalStateException("circuit breaker open"))
			.thenReturn(List.of("id-1"), emptyList())
			.thenThrow(new IllegalStateException());
		when(errandServiceMock.deleteSoftDeletedErrand(eq("id-1"), any())).thenReturn(true);

		final var account = worker.deleteSoftDeletedErrands();

		// The namespace after the one that could not be walked is walked all the same
		assertThat(account).hasValue("Deleting soft deleted errands was aborted in namespace FAILING for municipality 2281, BROKEN for municipality 2281");
		verify(namespaceConfigRepositoryMock).findAll();
		verify(errandsRepositoryMock, times(3)).findIdsAfter(any(), isNull(), eq(BATCH_SIZE));
		verify(errandsRepositoryMock).findIdsAfter(any(), eq("id-1"), eq(BATCH_SIZE));
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-1"), any());
		verifyNoMoreInteractions(namespaceConfigRepositoryMock, errandsRepositoryMock, errandServiceMock);
	}

	private static NamespaceConfigEntity config(final String namespace, final Integer softDeletedRetentionInDays) {
		final var config = NamespaceConfigEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withValue(NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_NOTIFICATION_TTL_IN_DAYS).withType(INTEGER).withValue("40"));

		if (softDeletedRetentionInDays != null) {
			config.withValue(NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_SOFT_DELETED_RETENTION_IN_DAYS).withType(INTEGER).withValue(String.valueOf(softDeletedRetentionInDays)));
		}
		return config;
	}
}

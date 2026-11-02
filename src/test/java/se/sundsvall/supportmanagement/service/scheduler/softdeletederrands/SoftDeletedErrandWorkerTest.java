package se.sundsvall.supportmanagement.service.scheduler.softdeletederrands;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.INTEGER;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_NOTIFICATION_TTL_IN_DAYS;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_SOFT_DELETED_RETENTION_IN_DAYS;
import static se.sundsvall.supportmanagement.service.scheduler.softdeletederrands.SoftDeletedErrandWorker.BATCH_SIZE;

@ExtendWith(MockitoExtension.class)
class SoftDeletedErrandWorkerTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String KEEPING_NAMESPACE = "KEEPING";
	private static final String NAMESPACE = "NAMESPACE";
	private static final String IMMEDIATE_NAMESPACE = "IMMEDIATE";
	private static final PageRequest BATCH = PageRequest.of(0, BATCH_SIZE);

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

	@Mock
	private ErrandService errandServiceMock;

	@Captor
	private ArgumentCaptor<OffsetDateTime> deletedBeforeCaptor;

	@InjectMocks
	private SoftDeletedErrandWorker worker;

	@Test
	void deleteSoftDeletedErrands() {
		final var firstBatch = IntStream.range(0, BATCH_SIZE).mapToObj("id-%03d"::formatted).toList();
		final var lastOfFirstBatch = firstBatch.getLast();

		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(config(KEEPING_NAMESPACE, null), config(NAMESPACE, 30), config(IMMEDIATE_NAMESPACE, 0)));
		when(errandsRepositoryMock.findIdsDeletedBefore(eq(NAMESPACE), eq(MUNICIPALITY_ID), deletedBeforeCaptor.capture(), eq(""), eq(BATCH))).thenReturn(firstBatch);
		when(errandsRepositoryMock.findIdsDeletedBefore(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(), eq(lastOfFirstBatch), eq(BATCH))).thenReturn(List.of("id-restored", "id-failing", "id-last"));
		when(errandsRepositoryMock.findIdsDeletedBefore(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(), eq("id-last"), eq(BATCH))).thenReturn(emptyList());
		when(errandsRepositoryMock.findIdsDeletedBefore(eq(IMMEDIATE_NAMESPACE), eq(MUNICIPALITY_ID), deletedBeforeCaptor.capture(), eq(""), eq(BATCH))).thenReturn(emptyList());
		firstBatch.forEach(id -> when(errandServiceMock.deleteSoftDeletedErrand(eq(id), any())).thenReturn(true));
		when(errandServiceMock.deleteSoftDeletedErrand(eq("id-restored"), any())).thenReturn(false);
		when(errandServiceMock.deleteSoftDeletedErrand(eq("id-failing"), any())).thenThrow(new IllegalStateException("locked decision"));
		when(errandServiceMock.deleteSoftDeletedErrand(eq("id-last"), any())).thenReturn(true);

		worker.deleteSoftDeletedErrands();

		assertThat(deletedBeforeCaptor.getAllValues()).satisfiesExactly(
			namespaceCutoff -> assertThat(namespaceCutoff).isCloseTo(OffsetDateTime.now().minusDays(30), within(5, SECONDS)),
			immediateCutoff -> assertThat(immediateCutoff).isCloseTo(OffsetDateTime.now(), within(5, SECONDS)));
		final var namespaceCutoff = deletedBeforeCaptor.getAllValues().getFirst();

		verify(namespaceConfigRepositoryMock).findAll();
		verify(errandsRepositoryMock).findIdsDeletedBefore(NAMESPACE, MUNICIPALITY_ID, namespaceCutoff, "", BATCH);
		verify(errandsRepositoryMock).findIdsDeletedBefore(NAMESPACE, MUNICIPALITY_ID, namespaceCutoff, lastOfFirstBatch, BATCH);
		verify(errandsRepositoryMock).findIdsDeletedBefore(NAMESPACE, MUNICIPALITY_ID, namespaceCutoff, "id-last", BATCH);
		verify(errandsRepositoryMock).findIdsDeletedBefore(IMMEDIATE_NAMESPACE, MUNICIPALITY_ID, deletedBeforeCaptor.getAllValues().getLast(), "", BATCH);
		firstBatch.forEach(id -> verify(errandServiceMock).deleteSoftDeletedErrand(id, namespaceCutoff));
		verify(errandServiceMock).deleteSoftDeletedErrand("id-restored", namespaceCutoff);
		verify(errandServiceMock).deleteSoftDeletedErrand("id-failing", namespaceCutoff);
		verify(errandServiceMock).deleteSoftDeletedErrand("id-last", namespaceCutoff);
		verifyNoMoreInteractions(namespaceConfigRepositoryMock, errandsRepositoryMock, errandServiceMock);
	}

	@Test
	void deleteSoftDeletedErrandsWhenAnErrandFailsWithoutMessage() {
		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(config(NAMESPACE, 30)));
		when(errandsRepositoryMock.findIdsDeletedBefore(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(), eq(""), eq(BATCH))).thenReturn(List.of("id-failing"));
		when(errandsRepositoryMock.findIdsDeletedBefore(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(), eq("id-failing"), eq(BATCH))).thenReturn(emptyList());
		when(errandServiceMock.deleteSoftDeletedErrand(eq("id-failing"), any())).thenThrow(new IllegalStateException());

		worker.deleteSoftDeletedErrands();

		verify(namespaceConfigRepositoryMock).findAll();
		verify(errandsRepositoryMock).findIdsDeletedBefore(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(), eq(""), eq(BATCH));
		verify(errandsRepositoryMock).findIdsDeletedBefore(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(), eq("id-failing"), eq(BATCH));
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-failing"), any());
		verifyNoMoreInteractions(namespaceConfigRepositoryMock, errandsRepositoryMock, errandServiceMock);
	}

	@Test
	void deleteSoftDeletedErrandsWhenNoNamespaceSetsARetention() {
		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(config(KEEPING_NAMESPACE, null)));

		worker.deleteSoftDeletedErrands();

		verify(namespaceConfigRepositoryMock).findAll();
		verifyNoMoreInteractions(namespaceConfigRepositoryMock);
		verifyNoInteractions(errandsRepositoryMock, errandServiceMock);
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

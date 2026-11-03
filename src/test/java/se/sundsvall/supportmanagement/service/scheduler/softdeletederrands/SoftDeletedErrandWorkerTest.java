package se.sundsvall.supportmanagement.service.scheduler.softdeletederrands;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.IdProjection;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;
import se.sundsvall.supportmanagement.service.ErrandService;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
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
		final var firstBatch = IntStream.range(0, BATCH_SIZE).mapToObj("id-%03d"::formatted).toArray(String[]::new);

		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(config("KEEPING", null), config("NAMESPACE", 30), config("IMMEDIATE", 0)));
		// Three batches for the namespace keeping its soft deleted errands for 30 days, the last of them empty, and two for
		// the one keeping them for none
		batches(ids(firstBatch), ids("id-restored", "id-failing", "id-last"), emptyList(), ids("id-immediate"), emptyList());
		when(errandServiceMock.deleteSoftDeletedErrand(any(), any())).thenReturn(true);
		when(errandServiceMock.deleteSoftDeletedErrand(eq("id-restored"), any())).thenReturn(false);
		when(errandServiceMock.deleteSoftDeletedErrand(eq("id-failing"), any())).thenThrow(new IllegalStateException("locked decision"));

		worker.deleteSoftDeletedErrands();

		verify(namespaceConfigRepositoryMock).findAll();
		verify(errandsRepositoryMock, times(5)).findBy(anySpecification(), any());
		// Every errand of the namespace keeping them for 30 days first, and the one of the namespace keeping them for none last
		verify(errandServiceMock, times(BATCH_SIZE + 4)).deleteSoftDeletedErrand(any(), deletedBeforeCaptor.capture());
		final var deletedBefores = deletedBeforeCaptor.getAllValues();
		assertThat(deletedBefores.subList(0, BATCH_SIZE + 3)).allSatisfy(deletedBefore -> assertThat(deletedBefore).isCloseTo(OffsetDateTime.now().minusDays(30), within(5, SECONDS)));
		assertThat(deletedBefores.getLast()).isCloseTo(OffsetDateTime.now(), within(5, SECONDS));
		List.of(firstBatch).forEach(id -> verify(errandServiceMock).deleteSoftDeletedErrand(eq(id), any()));
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-restored"), any());
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-failing"), any());
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-last"), any());
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-immediate"), any());
		verifyNoMoreInteractions(namespaceConfigRepositoryMock, errandsRepositoryMock, errandServiceMock);
	}

	@Test
	void deleteSoftDeletedErrandsWhenAnErrandFailsWithoutMessage() {
		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(config("NAMESPACE", 30)));
		batches(ids("id-failing"), emptyList());
		when(errandServiceMock.deleteSoftDeletedErrand(eq("id-failing"), any())).thenThrow(new IllegalStateException());

		worker.deleteSoftDeletedErrands();

		verify(namespaceConfigRepositoryMock).findAll();
		verify(errandsRepositoryMock, times(2)).findBy(anySpecification(), any());
		verify(errandServiceMock).deleteSoftDeletedErrand(eq("id-failing"), any());
		verifyNoMoreInteractions(namespaceConfigRepositoryMock, errandsRepositoryMock, errandServiceMock);
	}

	@Test
	void deleteSoftDeletedErrandsWhenNoNamespaceSetsARetention() {
		when(namespaceConfigRepositoryMock.findAll()).thenReturn(List.of(config("KEEPING", null)));

		worker.deleteSoftDeletedErrands();

		verify(namespaceConfigRepositoryMock).findAll();
		verifyNoMoreInteractions(namespaceConfigRepositoryMock);
		verifyNoInteractions(errandsRepositoryMock, errandServiceMock);
	}

	@SafeVarargs
	private void batches(final List<IdProjection>... batches) {
		var stubbing = doReturn(batches[0]);
		for (var index = 1; index < batches.length; index++) {
			stubbing = stubbing.doReturn(batches[index]);
		}
		stubbing.when(errandsRepositoryMock).findBy(anySpecification(), any());
	}

	private static List<IdProjection> ids(final String... ids) {
		return List.of(ids).stream()
			.map(id -> (IdProjection) new IdProjection() {

				@Override
				public String getId() {
					return id;
				}

				@Override
				public void setId(final String value) {
					// Nothing reads a value set here.
				}
			})
			.toList();
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

	/**
	 * Matches any specification, typed so that it picks one of the several counts and findBys of the repository.
	 */
	private static Specification<ErrandEntity> anySpecification() {
		return ArgumentMatchers.any();
	}
}

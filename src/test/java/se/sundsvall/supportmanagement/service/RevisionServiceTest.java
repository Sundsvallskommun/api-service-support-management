package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.service.mapper.RevisionMapper.toSerializedSnapshot;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.supportmanagement.integration.db.RevisionRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.RevisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;

@ExtendWith(MockitoExtension.class)
class RevisionServiceTest {

	@Mock
	private RevisionRepository repositoryMock;

	@InjectMocks
	private RevisionService service;

	@Captor
	private ArgumentCaptor<RevisionEntity> entityCaptor;

	@Test
	void shouldCreateRevisionWhenNoPreviousRevisionExists() {
		// Setup
		final var entityId = "entityId";

		// Mock
		when(repositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.empty());

		// Call
		service.createRevision(ErrandEntity.create().withId(entityId));

		// Assertions and verifications
		verify(repositoryMock).findFirstByEntityIdOrderByVersionDesc(entityId);
		verify(repositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo("{\"id\":\"entityId\"}");
		assertThat(entityCaptor.getValue().getVersion()).isZero();
	}

	@Test
	void shouldCreateRevisionWhenPreviousRevisionSnapshotDiffersFromCurrent() {
		// Setup
		final var entityId = "entityId";
		final var version = 1;

		// Mock
		when(repositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot("{\someKey\":\"someValue\"}")));

		// Call
		service.createRevision(ErrandEntity.create().withId(entityId));

		// Assertions and verifications
		verify(repositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo("{\"id\":\"entityId\"}");
		assertThat(entityCaptor.getValue().getVersion()).isEqualTo(version + 1);
	}

	@Test
	void shouldCreateRevisionWhenPreviousRevisionSnapshotIsNull() {
		// Setup
		final var entityId = "entityId";
		final var version = 2;

		// Mock
		when(repositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version)));

		// Call
		service.createRevision(ErrandEntity.create().withId(entityId));

		// Assertions and verifications
		verify(repositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo("{\"id\":\"entityId\"}");
		assertThat(entityCaptor.getValue().getVersion()).isEqualTo(version + 1);
	}

	@Test
	void shouldCreateRevisionWhenExceptionOccursInSnapshotComparison() {
		// Setup
		final var entityId = "entityId";
		final var version = 5;

		// Mock
		when(repositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot("{\"id\":\"entityId\"")));

		// Call
		service.createRevision(ErrandEntity.create().withId(entityId));

		// Assertions and verifications
		verify(repositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo("{\"id\":\"entityId\"}");
		assertThat(entityCaptor.getValue().getVersion()).isEqualTo(version + 1);
	}

	@Test
	void shouldNotCreateRevisionWhenPreviousRevisionSnapshotIsEqualToCurrent() {
		// Setup
		final var entityId = "entityId";
		final var version = 6;

		// Mock
		when(repositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot("{\"id\":\"entityId\"}")));

		// Call
		service.createRevision(ErrandEntity.create().withId(entityId));

		// Assertions and verifications
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void shouldNotCreateRevisionWhenNonComparedAttributesDiffers() {
		// Setup
		final var entityId = "entityId";
		final var version = 7;

		final var previousSnapshot = toSerializedSnapshot(createErrandEntity(entityId, 1));
		final var currentEntity = createErrandEntity(entityId, 0);

		// Mock
		when(repositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot(previousSnapshot)));

		// Call
		service.createRevision(currentEntity);

		// Assertions and verifications
		verify(repositoryMock, never()).save(any());
	}

	private ErrandEntity createErrandEntity(String entityId, int subtraction) {
		return ErrandEntity.create()
			.withId(entityId)
			.withCreated(OffsetDateTime.now().minusYears(subtraction))
			.withModified(OffsetDateTime.now().minusMonths(subtraction))
			.withTouched(OffsetDateTime.now().minusWeeks(subtraction))
			.withAttachments(List.of(AttachmentEntity.create().withId(UUID.randomUUID().toString())))
			.withStakeholders(List.of(StakeholderEntity.create().withId(RandomUtils.nextLong())));
	}
}

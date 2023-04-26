package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.service.mapper.RevisionMapper.toSerializedSnapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	@Spy
	private ObjectMapper objectMapperSpy;

	@BeforeEach
	void setup() {
		objectMapperSpy.setSerializationInclusion(Include.NON_NULL);
	}

	@Test
	void shouldCreateRevisionWhenNoPreviousRevisionExists() throws Exception {
		// Setup
		final var entityId = "entityId";
		final var entity = ErrandEntity.create().withId(entityId);
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(repositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.empty());
		when(repositoryMock.save(any(RevisionEntity.class))).thenReturn(RevisionEntity.create().withId(revisionId));

		// Call
		final var response = service.createRevision(entity);

		// Assertions and verifications
		verify(repositoryMock).findFirstByEntityIdOrderByVersionDesc(entityId);
		verify(repositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo(objectMapperSpy.writeValueAsString(entity));
		assertThat(entityCaptor.getValue().getVersion()).isZero();
		assertThat(response).isEqualTo(revisionId);
	}

	@Test
	void shouldCreateRevisionWhenPreviousRevisionSnapshotDiffersFromCurrent() throws Exception {
		// Setup
		final var entityId = "entityId";
		final var entity = ErrandEntity.create().withId(entityId);
		final var version = 1;
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(repositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot("{\someKey\":\"someValue\"}")));
		when(repositoryMock.save(any(RevisionEntity.class))).thenReturn(RevisionEntity.create().withId(revisionId));

		// Call
		final var response = service.createRevision(entity);

		// Assertions and verifications
		verify(repositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo(objectMapperSpy.writeValueAsString(entity));
		assertThat(entityCaptor.getValue().getVersion()).isEqualTo(version + 1);
		assertThat(response).isEqualTo(revisionId);
	}

	@Test
	void shouldCreateRevisionWhenPreviousRevisionSnapshotIsNull() throws Exception {
		// Setup
		final var entityId = "entityId";
		final var errandEntity = ErrandEntity.create().withId(entityId);
		final var version = 2;
		final var revisionEntity = RevisionEntity.create().withVersion(version);
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(repositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.of(revisionEntity));
		when(repositoryMock.save(any(RevisionEntity.class))).thenReturn(RevisionEntity.create().withId(revisionId));

		// Call
		final var response = service.createRevision(errandEntity);

		// Assertions and verifications
		verify(repositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo(objectMapperSpy.writeValueAsString(errandEntity));
		assertThat(entityCaptor.getValue().getVersion()).isEqualTo(version + 1);
		assertThat(response).isEqualTo(revisionId);
	}

	@Test
	void shouldCreateRevisionWhenExceptionOccursInSnapshotComparison() throws Exception {
		// Setup
		final var entityId = "entityId";
		final var entity = ErrandEntity.create().withId(entityId);
		final var version = 3;
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(repositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot("{\"id\":\"entityId\"")));
		when(repositoryMock.save(any(RevisionEntity.class))).thenReturn(RevisionEntity.create().withId(revisionId));

		// Call
		final var response = service.createRevision(entity);

		// Assertions and verifications
		verify(repositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo(objectMapperSpy.writeValueAsString(entity));
		assertThat(entityCaptor.getValue().getVersion()).isEqualTo(version + 1);
		assertThat(response).isEqualTo(revisionId);
	}

	@Test
	void shouldNotCreateRevisionWhenPreviousRevisionSnapshotIsEqualToCurrent() {
		// Setup
		final var entityId = "entityId";
		final var entity = ErrandEntity.create().withId(entityId);
		final var version = 4;

		// Mock
		when(repositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot("{\"id\":\"entityId\"}")));

		// Call
		service.createRevision(entity);

		// Assertions and verifications
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void shouldNotCreateRevisionWhenNonComparedAttributesDiffers() {
		// Setup
		final var entityId = "entityId";
		final var version = 5;

		final var previousSnapshot = toSerializedSnapshot(createErrandEntity(entityId));
		final var currentEntity = createErrandEntity(entityId);

		// Mock
		when(repositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot(previousSnapshot)));

		// Call
		service.createRevision(currentEntity);

		// Assertions and verifications
		verify(repositoryMock, never()).save(any());
	}

	private ErrandEntity createErrandEntity(String entityId) {
		return ErrandEntity.create()
			.withId(entityId)
			.withAttachments(List.of(AttachmentEntity.create().withId(UUID.randomUUID().toString())))
			.withStakeholders(List.of(StakeholderEntity.create().withId(RandomUtils.nextLong())));
	}
}

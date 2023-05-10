package se.sundsvall.supportmanagement.service;

import static java.time.Instant.ofEpochMilli;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.RevisionMapper.toSerializedSnapshot;

import java.time.OffsetDateTime;
import java.time.ZoneId;
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
import org.zalando.problem.ThrowableProblem;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import generated.se.sundsvall.notes.DifferenceResponse;
import se.sundsvall.supportmanagement.api.model.revision.Operation;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.RevisionRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.RevisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;

@ExtendWith(MockitoExtension.class)
class RevisionServiceTest {

	@Mock
	private RevisionRepository revisionRepositoryMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private NotesClient notesClientMock;

	@Mock
	private JsonProcessingException jsonProcessingExceptionMock;

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
	void shouldCreateErrandRevisionWhenNoPreviousRevisionExists() throws Exception {
		// Setup
		final var entityId = "entityId";
		final var entity = ErrandEntity.create().withId(entityId);
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(revisionRepositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.empty());
		when(revisionRepositoryMock.save(any(RevisionEntity.class))).thenReturn(RevisionEntity.create().withId(revisionId));

		// Call
		final var response = service.createErrandRevision(entity);

		// Assertions and verifications
		verify(revisionRepositoryMock).findFirstByEntityIdOrderByVersionDesc(entityId);
		verify(revisionRepositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo(objectMapperSpy.writeValueAsString(entity));
		assertThat(entityCaptor.getValue().getVersion()).isZero();
		assertThat(response).isNotNull().extracting(Revision::getId).isEqualTo(revisionId);
	}

	@Test
	void shouldCreateErrandRevisionWhenPreviousRevisionSnapshotDiffersFromCurrent() throws Exception {
		// Setup
		final var entityId = "entityId";
		final var entity = ErrandEntity.create().withId(entityId);
		final var version = 1;
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(revisionRepositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot("{\someKey\":\"someValue\"}")));
		when(revisionRepositoryMock.save(any(RevisionEntity.class))).thenReturn(RevisionEntity.create().withId(revisionId));

		// Call
		final var response = service.createErrandRevision(entity);

		// Assertions and verifications
		verify(revisionRepositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo(objectMapperSpy.writeValueAsString(entity));
		assertThat(entityCaptor.getValue().getVersion()).isEqualTo(version + 1);
		assertThat(response).isNotNull().extracting(Revision::getId).isEqualTo(revisionId);
	}

	@Test
	void shouldCreateErrandRevisionWhenPreviousRevisionSnapshotIsNull() throws Exception {
		// Setup
		final var entityId = "entityId";
		final var errandEntity = ErrandEntity.create().withId(entityId);
		final var version = 2;
		final var revisionEntity = RevisionEntity.create().withVersion(version);
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(revisionRepositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.of(revisionEntity));
		when(revisionRepositoryMock.save(any(RevisionEntity.class))).thenReturn(RevisionEntity.create().withId(revisionId));

		// Call
		final var response = service.createErrandRevision(errandEntity);

		// Assertions and verifications
		verify(revisionRepositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo(objectMapperSpy.writeValueAsString(errandEntity));
		assertThat(entityCaptor.getValue().getVersion()).isEqualTo(version + 1);
		assertThat(response).isNotNull().extracting(Revision::getId).isEqualTo(revisionId);
	}

	@Test
	void shouldCreateErrandRevisionWhenExceptionOccursInSnapshotComparison() throws Exception {
		// Setup
		final var entityId = "entityId";
		final var entity = ErrandEntity.create().withId(entityId);
		final var version = 3;
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(revisionRepositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot("{\"id\":\"entityId\"")));
		when(revisionRepositoryMock.save(any(RevisionEntity.class))).thenReturn(RevisionEntity.create().withId(revisionId));

		// Call
		final var response = service.createErrandRevision(entity);

		// Assertions and verifications
		verify(revisionRepositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo(objectMapperSpy.writeValueAsString(entity));
		assertThat(entityCaptor.getValue().getVersion()).isEqualTo(version + 1);
		assertThat(response).isNotNull().extracting(Revision::getId).isEqualTo(revisionId);
	}

	@Test
	void shouldNotErrandCreateRevisionWhenPreviousRevisionSnapshotIsEqualToCurrent() {
		// Setup
		final var entityId = "entityId";
		final var entity = ErrandEntity.create().withId(entityId);
		final var version = 4;

		// Mock
		when(revisionRepositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot("{\"id\":\"entityId\"}")));

		// Call
		service.createErrandRevision(entity);

		// Assertions and verifications
		verify(revisionRepositoryMock, never()).save(any());
	}

	@Test
	void shouldNotCreateErrandRevisionWhenNonComparedAttributesDiffers() {
		// Setup
		final var entityId = "entityId";
		final var version = 5;

		final var previousSnapshot = toSerializedSnapshot(createErrandEntity(entityId));
		final var currentEntity = createErrandEntity(entityId);

		// Mock
		when(revisionRepositoryMock.findFirstByEntityIdOrderByVersionDesc(entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot(previousSnapshot)));

		// Call
		service.createErrandRevision(currentEntity);

		// Assertions and verifications
		verify(revisionRepositoryMock, never()).save(any());
	}

	@Test
	void getErrandRevisionsForExistingErrand() {
		// Setup
		final var errandId = "errandId";

		// Mock
		when(errandsRepositoryMock.existsById(errandId)).thenReturn(true);
		when(revisionRepositoryMock.findAllByEntityIdOrderByVersion(errandId)).thenReturn(List.of(createRevisionEntity(), createRevisionEntity(), createRevisionEntity()));

		// Call
		final var result = service.getErrandRevisions(errandId);

		verify(errandsRepositoryMock).existsById(errandId);
		verify(revisionRepositoryMock).findAllByEntityIdOrderByVersion(errandId);

		assertThat(result).hasSize(3);
	}

	@Test
	void getErrandRevisionsForNonExistingErrand() {
		// Setup
		final var errandId = "errandId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.getErrandRevisions(errandId));

		// Assertions and verifications
		verify(errandsRepositoryMock).existsById(errandId);
		verifyNoInteractions(revisionRepositoryMock);

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found");
	}

	@Test
	void compareErrandRevisionVersionsNonExistingErrand() {
		// Setup
		final var errandId = "errandId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.compareErrandRevisionVersions(errandId, 0, 0));

		// Assertions and verifications
		verify(errandsRepositoryMock).existsById(errandId);
		verifyNoInteractions(revisionRepositoryMock);

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found");
	}

	@Test
	void getLatestErrandRevision() {
		// Setup
		final var errandId = "errandId";

		// Mock
		when(revisionRepositoryMock.findFirstByEntityIdOrderByVersionDesc(errandId)).thenReturn(Optional.of(createRevisionEntity()));

		// Call
		final var result = service.getLatestErrandRevision(errandId);

		// Assertions and verifications
		verify(revisionRepositoryMock).findFirstByEntityIdOrderByVersionDesc(errandId);
		assertThat(result).isNotNull();
	}

	@Test
	void getLatestErrandRevisionNonExistingErrand() {
		// Setup
		final var errandId = "errandId";

		// Call
		final var result = service.getLatestErrandRevision(errandId);

		// Assertions and verifications
		verify(revisionRepositoryMock).findFirstByEntityIdOrderByVersionDesc(errandId);
		assertThat(result).isNull();
	}

	@Test
	void getErrandRevisionByVersion() {
		// Setup
		final var errandId = "errandId";
		final var version = 123;

		// Mock
		when(revisionRepositoryMock.findByEntityIdAndVersion(errandId, version)).thenReturn(Optional.of(createRevisionEntity()));

		// Call
		final var result = service.getErrandRevisionByVersion(errandId, version);

		// Assertions and verifications
		verify(revisionRepositoryMock).findByEntityIdAndVersion(errandId, version);
		assertThat(result).isNotNull();
	}

	@Test
	void getErrandRevisionByVersionNonExistingErrand() {
		// Setup
		final var errandId = "errandId";
		final var version = 123;

		// Call
		final var result = service.getErrandRevisionByVersion(errandId, version);

		// Assertions and verifications
		verify(revisionRepositoryMock).findByEntityIdAndVersion(errandId, version);
		assertThat(result).isNull();
	}

	@Test
	void compareErrandRevisionVersionsNonExistingSourceVersion() {
		// Setup
		final var errandId = "errandId";
		final var sourceVersion = 5;
		final var targetVersion = 6;

		// Mock
		when(errandsRepositoryMock.existsById(errandId)).thenReturn(true);

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.compareErrandRevisionVersions(errandId, sourceVersion, targetVersion));

		// Assertions and verifications
		verify(errandsRepositoryMock).existsById(errandId);
		verify(revisionRepositoryMock).findByEntityIdAndVersion(errandId, sourceVersion);
		verify(revisionRepositoryMock, never()).findByEntityIdAndVersion(errandId, targetVersion);

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: The version requested for the source revision does not exist");
	}

	@Test
	void compareErrandRevisionVersionsNonExistingTargetVersion() {
		// Setup
		final var errandId = "errandId";
		final var sourceVersion = 5;
		final var targetVersion = 6;

		// Mock
		when(errandsRepositoryMock.existsById(errandId)).thenReturn(true);
		when(revisionRepositoryMock.findByEntityIdAndVersion(errandId, sourceVersion)).thenReturn(Optional.of(createRevisionEntity()));

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.compareErrandRevisionVersions(errandId, sourceVersion, targetVersion));

		// Assertions and verifications
		verify(errandsRepositoryMock).existsById(errandId);
		verify(revisionRepositoryMock).findByEntityIdAndVersion(errandId, sourceVersion);
		verify(revisionRepositoryMock).findByEntityIdAndVersion(errandId, targetVersion);

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: The version requested for the target revision does not exist");

	}

	@Test
	void compareErrandRevisionVersionsWithNoDiff() {
		// Setup
		final var errandId = "errandId";
		final var sourceVersion = 5;

		// Mock
		when(errandsRepositoryMock.existsById(errandId)).thenReturn(true);
		when(revisionRepositoryMock.findByEntityIdAndVersion(errandId, sourceVersion)).thenReturn(Optional.of(createRevisionEntity()));

		// Call
		final var result = service.compareErrandRevisionVersions(errandId, sourceVersion, sourceVersion);

		// Assertions and verifications
		verify(errandsRepositoryMock).existsById(errandId);
		verify(revisionRepositoryMock, times(2)).findByEntityIdAndVersion(errandId, sourceVersion);

		assertThat(result.getOperations()).isEmpty();
	}

	@Test
	void compareErrandRevisionVersionsWithDiff() {
		// Setup
		final var errandId = "errandId";
		final var sourceVersion = 5;
		final var targetVersion = 6;

		// Mock
		when(errandsRepositoryMock.existsById(errandId)).thenReturn(true);
		when(revisionRepositoryMock.findByEntityIdAndVersion(errandId, sourceVersion)).thenReturn(Optional.of(createRevisionEntity("key", "oldValue")));
		when(revisionRepositoryMock.findByEntityIdAndVersion(errandId, targetVersion)).thenReturn(Optional.of(createRevisionEntity("key", "newValue")));

		// Call
		final var result = service.compareErrandRevisionVersions(errandId, sourceVersion, targetVersion);

		// Assertions and verifications
		verify(errandsRepositoryMock).existsById(errandId);
		verify(revisionRepositoryMock).findByEntityIdAndVersion(errandId, sourceVersion);
		verify(revisionRepositoryMock).findByEntityIdAndVersion(errandId, targetVersion);

		assertThat(result.getOperations()).hasSize(1)
			.extracting(
				Operation::getOp,
				Operation::getPath,
				Operation::getValue,
				Operation::getFromValue)
			.containsExactly(tuple(
				"replace",
				"/key",
				"newValue",
				"oldValue"));
	}

	@Test
	void compareErrandRevisionVersionsThrowsException() throws Exception {
		// Setup
		final var errandId = "errandId";
		final var sourceVersion = 5;
		final var targetVersion = 6;

		// Mock
		when(errandsRepositoryMock.existsById(errandId)).thenReturn(true);
		when(revisionRepositoryMock.findByEntityIdAndVersion(errandId, sourceVersion)).thenReturn(Optional.of(createRevisionEntity("key", "oldValue")));
		when(revisionRepositoryMock.findByEntityIdAndVersion(errandId, targetVersion)).thenReturn(Optional.of(createRevisionEntity("key", "newValue\"")));

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.compareErrandRevisionVersions(errandId, sourceVersion, targetVersion));

		// Assertions and verifications
		verify(errandsRepositoryMock).existsById(errandId);
		verify(revisionRepositoryMock).findByEntityIdAndVersion(errandId, sourceVersion);
		verify(revisionRepositoryMock).findByEntityIdAndVersion(errandId, targetVersion);

		assertThat(e.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(e.getMessage()).isEqualTo("Internal Server Error: An error occured when comparing version 5 to version 6 of entityId 'errandId'");

	}

	@Test
	void getNoteRevisionsForExistingErrand() {
		// Setup
		final var errandId = "errandId";
		final var noteId = "noteId";

		// Mock
		when(errandsRepositoryMock.existsById(errandId)).thenReturn(true);
		when(notesClientMock.findAllNoteRevisions(noteId)).thenReturn(List.of(new generated.se.sundsvall.notes.Revision()));

		// Call
		final var result = service.getNoteRevisions(errandId, noteId);

		// Assertions and verifications
		verify(errandsRepositoryMock).existsById(errandId);
		verify(notesClientMock).findAllNoteRevisions(noteId);

		assertThat(result).hasSize(1);
	}

	@Test
	void getNoteRevisionsForNonExistingErrand() {
		// Setup
		final var errandId = "errandId";
		final var noteId = "noteId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.getNoteRevisions(errandId, noteId));

		// Assertions and verifications
		verify(errandsRepositoryMock).existsById(errandId);
		verifyNoInteractions(notesClientMock);

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found");
	}

	@Test
	void compareNoteRevisionVersionsExistingErrand() {
		// Setup
		final var errandId = "errandId";
		final var noteId = "noteId";
		final var sourceVersion = 1;
		final var targetVersion = 2;

		// Mock
		when(errandsRepositoryMock.existsById(errandId)).thenReturn(true);
		when(notesClientMock.compareNoteRevisions(noteId, sourceVersion, targetVersion)).thenReturn(new DifferenceResponse().addOperationsItem(new generated.se.sundsvall.notes.Operation()));

		// Call
		final var result = service.compareNoteRevisionVersions(errandId, noteId, sourceVersion, targetVersion);

		// Assertions and verifications
		verify(errandsRepositoryMock).existsById(errandId);
		verify(notesClientMock).compareNoteRevisions(noteId, sourceVersion, targetVersion);

		assertThat(result).isNotNull();
		assertThat(result.getOperations()).hasSize(1);
	}

	@Test
	void compareNoteRevisionVersionsNonExistingErrand() {
		// Setup
		final var errandId = "errandId";
		final var noteId = "noteId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.compareNoteRevisionVersions(errandId, noteId, 0, 1));

		// Assertions and verifications
		verify(errandsRepositoryMock).existsById(errandId);
		verifyNoInteractions(notesClientMock);

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found");
	}

	private ErrandEntity createErrandEntity(String entityId) {
		return ErrandEntity.create()
			.withId(entityId)
			.withModified(OffsetDateTime.ofInstant(ofEpochMilli(RandomUtils.nextLong()), ZoneId.systemDefault()))
			.withTouched(OffsetDateTime.ofInstant(ofEpochMilli(RandomUtils.nextLong()), ZoneId.systemDefault()))
			.withAttachments(List.of(AttachmentEntity.create()
				.withId(UUID.randomUUID().toString())
				.withModified(OffsetDateTime.ofInstant(ofEpochMilli(RandomUtils.nextLong()), ZoneId.systemDefault()))
				.withFile(RandomUtils.nextBytes(30))))
			.withStakeholders(List.of(StakeholderEntity.create().withId(RandomUtils.nextLong())));
	}

	private RevisionEntity createRevisionEntity(String key, String value) {
		return createRevisionEntity()
			.withSerializedSnapshot("{\"" + key + "\": \"" + value + "\"}");
	}

	private RevisionEntity createRevisionEntity() {
		return RevisionEntity.create()
			.withCreated(OffsetDateTime.now())
			.withEntityId("entityId")
			.withEntityType("EntityType")
			.withId("revisionId")
			.withSerializedSnapshot("{}")
			.withVersion(0);
	}
}

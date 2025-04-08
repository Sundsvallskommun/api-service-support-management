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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import generated.se.sundsvall.notes.DifferenceResponse;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mariadb.jdbc.MariaDbBlob;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.revision.Operation;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.RevisionRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.RevisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;

@ExtendWith(MockitoExtension.class)
class RevisionServiceTest {

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private RevisionRepository revisionRepositoryMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private NotesClient notesClientMock;

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
		final var entity = ErrandEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID).withId(entityId);
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, entityId)).thenReturn(Optional.empty());
		when(revisionRepositoryMock.save(any(RevisionEntity.class))).thenReturn(RevisionEntity.create().withId(revisionId));

		// Call
		final var response = service.createErrandRevision(entity);

		// Assertions and verifications
		verify(revisionRepositoryMock).findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, entityId);
		verify(revisionRepositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo(objectMapperSpy.writeValueAsString(entity));
		assertThat(entityCaptor.getValue().getVersion()).isZero();
		assertThat(response.latest()).isNotNull().extracting(Revision::getId).isEqualTo(revisionId);
	}

	@Test
	void shouldCreateErrandRevisionWhenPreviousRevisionSnapshotDiffersFromCurrent() throws Exception {
		// Setup
		final var entityId = "entityId";
		final var entity = ErrandEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID).withId(entityId);
		final var version = 1;
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot(
			"{ omeKey\":\"someValue\"}")));
		when(revisionRepositoryMock.save(any(RevisionEntity.class))).thenReturn(RevisionEntity.create().withId(revisionId));

		// Call
		final var response = service.createErrandRevision(entity);

		// Assertions and verifications
		verify(revisionRepositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo(objectMapperSpy.writeValueAsString(entity));
		assertThat(entityCaptor.getValue().getVersion()).isEqualTo(version + 1);
		assertThat(response.previous()).isNotNull().extracting(Revision::getVersion).isEqualTo(version);
		assertThat(response.latest()).isNotNull().extracting(Revision::getId).isEqualTo(revisionId);
	}

	@Test
	void shouldCreateErrandRevisionWhenPreviousRevisionSnapshotIsNull() throws Exception {
		// Setup
		final var entityId = "entityId";
		final var errandEntity = ErrandEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID).withId(entityId);
		final var version = 2;
		final var revisionEntity = RevisionEntity.create().withVersion(version);
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, entityId)).thenReturn(Optional.of(revisionEntity));
		when(revisionRepositoryMock.save(any(RevisionEntity.class))).thenReturn(RevisionEntity.create().withId(revisionId));

		// Call
		final var response = service.createErrandRevision(errandEntity);

		// Assertions and verifications
		verify(revisionRepositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo(objectMapperSpy.writeValueAsString(errandEntity));
		assertThat(entityCaptor.getValue().getVersion()).isEqualTo(version + 1);
		assertThat(response.previous()).isNotNull().extracting(Revision::getVersion).isEqualTo(version);
		assertThat(response.latest()).isNotNull().extracting(Revision::getId).isEqualTo(revisionId);
	}

	@Test
	void shouldCreateErrandRevisionWhenExceptionOccursInSnapshotComparison() throws Exception {
		// Setup
		final var entityId = "entityId";
		final var entity = ErrandEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID).withId(entityId);
		final var version = 3;
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot(
			"{\"id\":\"entityId\"")));
		when(revisionRepositoryMock.save(any(RevisionEntity.class))).thenReturn(RevisionEntity.create().withId(revisionId));

		// Call
		final var response = service.createErrandRevision(entity);

		// Assertions and verifications
		verify(revisionRepositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo(objectMapperSpy.writeValueAsString(entity));
		assertThat(entityCaptor.getValue().getVersion()).isEqualTo(version + 1);
		assertThat(response.previous()).isNotNull().extracting(Revision::getVersion).isEqualTo(version);
		assertThat(response.latest()).isNotNull().extracting(Revision::getId).isEqualTo(revisionId);
	}

	@Test
	void shouldNotErrandCreateRevisionWhenPreviousRevisionSnapshotIsEqualToCurrent() {
		// Setup
		final var entityId = "entityId";
		final var entity = ErrandEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID).withId(entityId);
		final var version = 4;

		// Mock
		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, entityId)).thenReturn(Optional.of(RevisionEntity.create().withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE)
			.withVersion(version)
			.withSerializedSnapshot("{\"id\":\"entityId\", \"namespace\":\"" + NAMESPACE + "\", \"municipalityId\":\"" + MUNICIPALITY_ID + "\"}")));

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
		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, entityId)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot(previousSnapshot)));

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
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(revisionRepositoryMock.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, errandId)).thenReturn(List.of(createRevisionEntity(), createRevisionEntity(), createRevisionEntity()));

		// Call
		final var result = service.getErrandRevisions(NAMESPACE, MUNICIPALITY_ID, errandId);

		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID);
		verify(revisionRepositoryMock).findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, errandId);

		assertThat(result).hasSize(3);
	}

	@Test
	void getErrandRevisionsForNonExistingErrand() {
		// Setup
		final var errandId = "errandId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.getErrandRevisions(NAMESPACE, MUNICIPALITY_ID, errandId));

		// Assertions and verifications
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(revisionRepositoryMock);

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found");
	}

	@Test
	void compareErrandRevisionVersionsNonExistingErrand() {
		// Setup
		final var errandId = "errandId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.compareErrandRevisionVersions(NAMESPACE, MUNICIPALITY_ID, errandId, 0, 0));

		// Assertions and verifications
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(revisionRepositoryMock);

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found");
	}

	@Test
	void getLatestErrandRevision() {
		// Setup
		final var errandId = "errandId";

		// Mock
		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, errandId)).thenReturn(Optional.of(createRevisionEntity()));

		// Call
		final var result = service.getLatestErrandRevision(NAMESPACE, MUNICIPALITY_ID, errandId);

		// Assertions and verifications
		verify(revisionRepositoryMock).findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, errandId);
		assertThat(result).isNotNull();
	}

	@Test
	void getLatestErrandRevisionNonExistingErrand() {
		// Setup
		final var errandId = "errandId";

		// Call
		final var result = service.getLatestErrandRevision(NAMESPACE, MUNICIPALITY_ID, errandId);

		// Assertions and verifications
		verify(revisionRepositoryMock).findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, errandId);
		assertThat(result).isNull();
	}

	@Test
	void getErrandRevisionByVersion() {
		// Setup
		final var errandId = "errandId";
		final var version = 123;

		// Mock
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, version)).thenReturn(Optional.of(createRevisionEntity()));

		// Call
		final var result = service.getErrandRevisionByVersion(NAMESPACE, MUNICIPALITY_ID, errandId, version);

		// Assertions and verifications
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, version);
		assertThat(result).isNotNull();
	}

	@Test
	void getErrandRevisionByVersionNonExistingErrand() {
		// Setup
		final var errandId = "errandId";
		final var version = 123;

		// Call
		final var result = service.getErrandRevisionByVersion(NAMESPACE, MUNICIPALITY_ID, errandId, version);

		// Assertions and verifications
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, version);
		assertThat(result).isNull();
	}

	@Test
	void compareErrandRevisionVersionsNonExistingSourceVersion() {
		// Setup
		final var errandId = "errandId";
		final var sourceVersion = 5;
		final var targetVersion = 6;

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.compareErrandRevisionVersions(NAMESPACE, MUNICIPALITY_ID, errandId, sourceVersion, targetVersion));

		// Assertions and verifications
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID);
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, sourceVersion);
		verify(revisionRepositoryMock, never()).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, targetVersion);

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
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, sourceVersion)).thenReturn(Optional.of(createRevisionEntity()));

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.compareErrandRevisionVersions(NAMESPACE, MUNICIPALITY_ID, errandId, sourceVersion, targetVersion));

		// Assertions and verifications
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID);
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, sourceVersion);
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, targetVersion);

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: The version requested for the target revision does not exist");

	}

	@Test
	void compareErrandRevisionVersionsWithNoDiff() {
		// Setup
		final var errandId = "errandId";
		final var sourceVersion = 5;

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, sourceVersion)).thenReturn(Optional.of(createRevisionEntity()));

		// Call
		final var result = service.compareErrandRevisionVersions(NAMESPACE, MUNICIPALITY_ID, errandId, sourceVersion, sourceVersion);

		// Assertions and verifications
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID);
		verify(revisionRepositoryMock, times(2)).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, sourceVersion);

		assertThat(result.getOperations()).isEmpty();
	}

	@Test
	void compareErrandRevisionVersionsWithDiff() {
		// Setup
		final var errandId = "errandId";
		final var sourceVersion = 5;
		final var targetVersion = 6;

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, sourceVersion)).thenReturn(Optional.of(createRevisionEntity("key", "oldValue")));
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, targetVersion)).thenReturn(Optional.of(createRevisionEntity("key", "newValue")));

		// Call
		final var result = service.compareErrandRevisionVersions(NAMESPACE, MUNICIPALITY_ID, errandId, sourceVersion, targetVersion);

		// Assertions and verifications
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID);
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, sourceVersion);
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, targetVersion);

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
	void compareErrandRevisionVersionsThrowsException() {
		// Setup
		final var errandId = "errandId";
		final var sourceVersion = 5;
		final var targetVersion = 6;

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, sourceVersion)).thenReturn(Optional.of(createRevisionEntity("key", "oldValue")));
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, targetVersion)).thenReturn(Optional.of(createRevisionEntity("key", "newValue\"")));

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.compareErrandRevisionVersions(NAMESPACE, MUNICIPALITY_ID, errandId, sourceVersion, targetVersion));

		// Assertions and verifications
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID);
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, sourceVersion);
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, errandId, targetVersion);

		assertThat(e.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(e.getMessage()).isEqualTo("Internal Server Error: An error occurred when comparing version 5 to version 6 of entityId 'errandId'");

	}

	@Test
	void getNoteRevisionsForExistingErrand() {
		// Setup
		final var errandId = "errandId";
		final var noteId = "noteId";

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(notesClientMock.findAllNoteRevisions(MUNICIPALITY_ID, noteId)).thenReturn(List.of(new generated.se.sundsvall.notes.Revision()));

		// Call
		final var result = service.getNoteRevisions(NAMESPACE, MUNICIPALITY_ID, errandId, noteId);

		// Assertions and verifications
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID);
		verify(notesClientMock).findAllNoteRevisions(MUNICIPALITY_ID, noteId);

		assertThat(result).hasSize(1);
	}

	@Test
	void getNoteRevisionsForNonExistingErrand() {
		// Setup
		final var errandId = "errandId";
		final var noteId = "noteId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.getNoteRevisions(NAMESPACE, MUNICIPALITY_ID, errandId, noteId));

		// Assertions and verifications
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID);
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
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(notesClientMock.compareNoteRevisions(MUNICIPALITY_ID, noteId, sourceVersion, targetVersion)).thenReturn(new DifferenceResponse().addOperationsItem(new generated.se.sundsvall.notes.Operation()));

		// Call
		final var result = service.compareNoteRevisionVersions(NAMESPACE, MUNICIPALITY_ID, errandId, noteId, sourceVersion, targetVersion);

		// Assertions and verifications
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID);
		verify(notesClientMock).compareNoteRevisions(MUNICIPALITY_ID, noteId, sourceVersion, targetVersion);

		assertThat(result).isNotNull();
		assertThat(result.getOperations()).hasSize(1);
	}

	@Test
	void compareNoteRevisionVersionsNonExistingErrand() {
		// Setup
		final var errandId = "errandId";
		final var noteId = "noteId";

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.compareNoteRevisionVersions(NAMESPACE, MUNICIPALITY_ID, errandId, noteId, 0, 1));

		// Assertions and verifications
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(notesClientMock);

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found");
	}

	private ErrandEntity createErrandEntity(final String entityId) {
		final var randomBytes = new byte[30];
		new Random().nextBytes(randomBytes);

		return ErrandEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withId(entityId)
			.withModified(OffsetDateTime.ofInstant(ofEpochMilli(new Random().nextLong()), ZoneId.systemDefault()))
			.withTouched(OffsetDateTime.ofInstant(ofEpochMilli(new Random().nextLong()), ZoneId.systemDefault()))
			.withAttachments(List.of(AttachmentEntity.create()
				.withId(UUID.randomUUID().toString())
				.withModified(OffsetDateTime.ofInstant(ofEpochMilli(new Random().nextLong()), ZoneId.systemDefault()))
				.withAttachmentData(AttachmentDataEntity.create().withFile(new MariaDbBlob(randomBytes)))))
			.withStakeholders(List.of(StakeholderEntity.create().withId(new Random().nextLong())));
	}

	private RevisionEntity createRevisionEntity(final String key, final String value) {
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

package se.sundsvall.supportmanagement.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import generated.se.sundsvall.notes.DifferenceResponse;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mariadb.jdbc.MariaDbBlob;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.revision.Operation;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.RevisionRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.IdProjection;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.RevisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static java.time.Instant.ofEpochMilli;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.RevisionMapper.toSerializedSnapshot;

@ExtendWith(MockitoExtension.class)
class RevisionServiceTest {

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String ERRAND_ID = "errandId";

	@Mock
	private RevisionRepository revisionRepositoryMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private NotesClient notesClientMock;

	@Mock
	private ErrandNoteService errandNoteServiceMock;

	@Mock
	private ChunkedDeleter chunkedDeleterMock;

	@InjectMocks
	private RevisionService service;

	@Captor
	private ArgumentCaptor<RevisionEntity> entityCaptor;

	@Spy
	private ObjectMapper objectMapperSpy = JsonMapper.builder()
		.changeDefaultPropertyInclusion(c -> c.withValueInclusion(JsonInclude.Include.NON_NULL))
		.build();

	@Test
	void shouldCreateErrandRevisionWhenNoPreviousRevisionExists() throws Exception {
		// Setup
		final var entity = ErrandEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID).withId(ERRAND_ID);
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(Optional.empty());
		when(revisionRepositoryMock.save(any(RevisionEntity.class))).thenReturn(RevisionEntity.create().withId(revisionId));

		// Call
		final var response = service.createErrandRevision(entity);

		// Assertions and verifications
		verify(revisionRepositoryMock).findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		verify(revisionRepositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("ErrandEntity");
		assertThat(entityCaptor.getValue().getSerializedSnapshot()).isEqualTo(objectMapperSpy.writeValueAsString(entity));
		assertThat(entityCaptor.getValue().getVersion()).isZero();
		assertThat(response.latest()).isNotNull().extracting(Revision::getId).isEqualTo(revisionId);
	}

	@Test
	void shouldCreateErrandRevisionWhenPreviousRevisionSnapshotDiffersFromCurrent() throws Exception {
		// Setup
		final var entity = ErrandEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID).withId(ERRAND_ID);
		final var version = 1;
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot(
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
		final var errandEntity = ErrandEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID).withId(ERRAND_ID);
		final var version = 2;
		final var revisionEntity = RevisionEntity.create().withVersion(version);
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(Optional.of(revisionEntity));
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
		final var entity = ErrandEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID).withId(ERRAND_ID);
		final var version = 3;
		final var revisionId = UUID.randomUUID().toString();

		// Mock
		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot(
			String.format("{\"id\":\"%s}\"", ERRAND_ID))));
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
		final var entity = ErrandEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID).withId(ERRAND_ID);
		final var version = 4;

		// Mock
		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(Optional.of(RevisionEntity.create().withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE)
			.withVersion(version)
			.withSerializedSnapshot("{\"id\":\"" + ERRAND_ID + "\", \"namespace\":\"" + NAMESPACE + "\", \"municipalityId\":\"" + MUNICIPALITY_ID + "\"}")));

		// Call
		service.createErrandRevision(entity);

		// Assertions and verifications
		verify(revisionRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that what a snapshot written earlier carries of the loaded state is not read as a change, since snapshots written now leave it out")
	void shouldNotCreateErrandRevisionWhenOnlyTheLoadedStateOfTheLastSnapshotDiffers() {
		final var entity = ErrandEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID).withId(ERRAND_ID)
			.withLabels(List.of(ErrandLabelEmbeddable.create()
				.withMetadataLabelId("label-id")
				.withMetadataLabel(MetadataLabelEntity.create().withId("label-id").withDisplayName("Ansokan"))));
		final var earlierSnapshot = """
			{"id":"%s","namespace":"%s","municipalityId":"%s","tempPreviousStatus":"STATUS-1",
			 "labels":[{"metadataLabelId":"label-id","metadataLabel":{"id":"label-id","displayName":"Ansokan","metadataLabels":[]}}]}"""
			.formatted(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);

		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.thenReturn(Optional.of(RevisionEntity.create().withVersion(3).withSerializedSnapshot(earlierSnapshot)));

		assertThat(service.createErrandRevision(entity)).isNull();

		verify(revisionRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that an errand just written reads like the same errand just read: empty collections are no collections, and labels and tags come in any order")
	void shouldNotCreateErrandRevisionWhenOnlyEmptyCollectionsAndTheOrderOfUnorderedOnesDiffer() {
		final var justRead = ErrandEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID).withId(ERRAND_ID)
			.withLabels(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId("b"), ErrandLabelEmbeddable.create().withMetadataLabelId("a")))
			.withExternalTags(List.of(DbExternalTag.create().withKey("z").withValue("1"), DbExternalTag.create().withKey("y").withValue("2")))
			.withActions(List.of())
			.withNotifications(List.of())
			.withStakeholders(List.of(StakeholderEntity.create().withFirstName("x").withContactChannels(List.of())));
		final var justWritten = ErrandEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID).withId(ERRAND_ID)
			.withLabels(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId("a"), ErrandLabelEmbeddable.create().withMetadataLabelId("b")))
			.withExternalTags(List.of(DbExternalTag.create().withKey("y").withValue("2"), DbExternalTag.create().withKey("z").withValue("1")))
			.withStakeholders(List.of(StakeholderEntity.create().withFirstName("x")));

		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.thenReturn(Optional.of(RevisionEntity.create().withVersion(0).withSerializedSnapshot(toSerializedSnapshot(justWritten))));

		assertThat(service.createErrandRevision(justRead)).isNull();

		verify(revisionRepositoryMock, never()).save(any());
	}

	@Test
	@DisplayName("Verification that a diff leaves out a change of order in the collections that have none of their own, and still shows a collection going from none to empty")
	void compareErrandRevisionVersionsIgnoresTheOrderOfUnorderedCollections() {
		final var before = """
			{"labels":[{"metadataLabelId":"b"},{"metadataLabelId":"a"}],"accessLabels":[{"metadataLabelId":"b"},{"metadataLabelId":"a"}],
			 "externalTags":[{"key":"z","value":"1"},{"key":"y","value":"2"}]}""";
		final var after = """
			{"labels":[{"metadataLabelId":"a"},{"metadataLabelId":"b"}],"accessLabels":[{"metadataLabelId":"a"},{"metadataLabelId":"b"}],
			 "externalTags":[{"key":"y","value":"2"},{"key":"z","value":"1"}],"actions":[]}""";

		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, 0)).thenReturn(Optional.of(createRevisionEntity().withSerializedSnapshot(before)));
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, 1)).thenReturn(Optional.of(createRevisionEntity().withSerializedSnapshot(after)));

		assertThat(service.compareErrandRevisionVersions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, 0, 1).getOperations())
			.extracting(Operation::getOp, Operation::getPath)
			.containsExactly(tuple("add", "/actions"));
	}

	@Test
	@DisplayName("Verification that the order of a list that has one is still a change, and so is a label that is really gone")
	void compareErrandRevisionVersionsStillSeesRealChangesToCollections() {
		final var before = """
			{"parameters":[{"key":"a"},{"key":"b"}],"labels":[{"metadataLabelId":"a"},{"metadataLabelId":"b"}]}""";
		final var after = """
			{"parameters":[{"key":"b"},{"key":"a"}],"labels":[{"metadataLabelId":"b"}]}""";

		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, 0)).thenReturn(Optional.of(createRevisionEntity().withSerializedSnapshot(before)));
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, 1)).thenReturn(Optional.of(createRevisionEntity().withSerializedSnapshot(after)));

		assertThat(service.compareErrandRevisionVersions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, 0, 1).getOperations())
			.extracting(Operation::getPath)
			.contains("/labels/0")
			.anyMatch(path -> path.startsWith("/parameters/"));
	}

	@Test
	void shouldNotCreateErrandRevisionWhenNonComparedAttributesDiffers() {
		// Setup
		final var version = 5;

		final var previousSnapshot = toSerializedSnapshot(createErrandEntity());
		final var currentEntity = createErrandEntity();

		// Mock
		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(Optional.of(RevisionEntity.create().withVersion(version).withSerializedSnapshot(previousSnapshot)));

		// Call
		service.createErrandRevision(currentEntity);

		// Assertions and verifications
		verify(revisionRepositoryMock, never()).save(any());
	}

	@Test
	void getErrandRevisionsForExistingErrand() {
		// Mock
		when(revisionRepositoryMock.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(List.of(createRevisionEntity(), createRevisionEntity(), createRevisionEntity()));

		// Call
		final var result = service.getErrandRevisions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.REVISION, LR);
		verify(revisionRepositoryMock).findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		assertThat(result).hasSize(3);
	}

	@Test
	void getLatestErrandRevision() {
		// Setup
		final var errandEntity = ErrandEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withId(ERRAND_ID);

		// Mock
		when(revisionRepositoryMock.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(Optional.of(createRevisionEntity()));

		// Call
		final var result = service.getLatestErrandRevision(errandEntity);

		// Assertions and verifications
		verify(revisionRepositoryMock).findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		assertThat(result).isNotNull();
	}

	@Test
	void getLatestErrandRevisionNonExistingErrand() {
		// Setup
		final var errandEntity = ErrandEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withId(ERRAND_ID);

		// Call
		final var result = service.getLatestErrandRevision(errandEntity);

		// Assertions and verifications
		verify(revisionRepositoryMock).findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		assertThat(result).isNull();
	}

	@Test
	void getErrandRevisionByVersion() {
		// Setup
		final var version = 123;

		// Mock
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, version)).thenReturn(Optional.of(createRevisionEntity()));

		// Call
		final var result = service.getErrandRevisionByVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, version);

		// Assertions and verifications
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, version);
		assertThat(result).isNotNull();
	}

	@Test
	void getErrandRevisionByVersionNonExistingErrand() {
		// Setup
		final var version = 123;

		// Call
		final var result = service.getErrandRevisionByVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, version);

		// Assertions and verifications
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, version);
		assertThat(result).isNull();
	}

	@Test
	void compareErrandRevisionVersionsNonExistingSourceVersion() {
		// Setup
		final var sourceVersion = 5;
		final var targetVersion = 6;

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.compareErrandRevisionVersions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion, targetVersion));

		// Assertions and verifications
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.REVISION, LR);
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion);
		verify(revisionRepositoryMock, never()).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, targetVersion);

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: The version requested for the source revision does not exist");
	}

	@Test
	void compareErrandRevisionVersionsNonExistingTargetVersion() {
		// Setup
		final var sourceVersion = 5;
		final var targetVersion = 6;

		// Mock
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion)).thenReturn(Optional.of(createRevisionEntity()));

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.compareErrandRevisionVersions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion, targetVersion));

		// Assertions and verifications
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.REVISION, LR);
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion);
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, targetVersion);

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: The version requested for the target revision does not exist");

	}

	@Test
	void compareErrandRevisionVersionsWithNoDiff() {
		// Setup
		final var sourceVersion = 5;

		// Mock
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion)).thenReturn(Optional.of(createRevisionEntity()));

		// Call
		final var result = service.compareErrandRevisionVersions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion, sourceVersion);

		// Assertions and verifications
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.REVISION, LR);
		verify(revisionRepositoryMock, times(2)).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion);

		assertThat(result.getOperations()).isEmpty();
	}

	@Test
	void compareErrandRevisionVersionsWithDiff() {
		// Setup
		final var sourceVersion = 5;
		final var targetVersion = 6;

		// Mock
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion)).thenReturn(Optional.of(createRevisionEntity("key", "oldValue")));
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, targetVersion)).thenReturn(Optional.of(createRevisionEntity("key", "newValue")));

		// Call
		final var result = service.compareErrandRevisionVersions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion, targetVersion);

		// Assertions and verifications
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.REVISION, LR);
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion);
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, targetVersion);

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
	@DisplayName("Verification that comparing a revision written with label metadata to one written without it shows no difference, since only the metadata differs")
	void compareErrandRevisionVersionsIgnoresTheLabelMetadataOfEarlierRevisions() {
		final var sourceVersion = 1;
		final var targetVersion = 2;

		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion)).thenReturn(Optional.of(createRevisionEntity()
			.withSerializedSnapshot("{\"labels\":[{\"metadataLabelId\":\"label-id\",\"metadataLabel\":{\"id\":\"label-id\",\"displayName\":\"Ansokan\"}}]}")));
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, targetVersion)).thenReturn(Optional.of(createRevisionEntity()
			.withSerializedSnapshot("{\"labels\":[{\"metadataLabelId\":\"label-id\"}]}")));

		final var result = service.compareErrandRevisionVersions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion, targetVersion);

		assertThat(result.getOperations()).isEmpty();
	}

	@Test
	void compareErrandRevisionVersionsThrowsException() {
		// Setup
		final var sourceVersion = 5;
		final var targetVersion = 6;

		// Mock
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion)).thenReturn(Optional.of(createRevisionEntity("key", "oldValue")));
		when(revisionRepositoryMock.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, targetVersion)).thenReturn(Optional.of(createRevisionEntity("key", "newValue\"")));

		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> service.compareErrandRevisionVersions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion, targetVersion));

		// Assertions and verifications
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.REVISION, LR);
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, sourceVersion);
		verify(revisionRepositoryMock).findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, targetVersion);

		assertThat(e.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(e.getMessage()).isEqualTo("Internal Server Error: An error occurred when comparing version 5 to version 6 of entityId 'errandId'");

	}

	@Test
	void getNoteRevisionsForExistingErrand() {
		// Setup
		final var noteId = "noteId";

		// Mock
		when(notesClientMock.findAllNoteRevisions(MUNICIPALITY_ID, noteId)).thenReturn(List.of(new generated.se.sundsvall.notes.Revision()));

		// Call
		final var result = service.getNoteRevisions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, noteId);

		// Assertions and verifications
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.NOTE_REVISION, LR);
		verify(notesClientMock).findAllNoteRevisions(MUNICIPALITY_ID, noteId);

		assertThat(result).hasSize(1);
	}

	@Test
	void compareNoteRevisionVersionsExistingErrand() {
		// Setup
		final var noteId = "noteId";
		final var sourceVersion = 1;
		final var targetVersion = 2;

		// Mock
		when(notesClientMock.compareNoteRevisions(MUNICIPALITY_ID, noteId, sourceVersion, targetVersion)).thenReturn(new DifferenceResponse().addOperationsItem(new generated.se.sundsvall.notes.Operation()));

		// Call
		final var result = service.compareNoteRevisionVersions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, noteId, sourceVersion, targetVersion);

		// Assertions and verifications
		verify(accessControlServiceMock).verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.NOTE_REVISION, LR);
		verify(notesClientMock).compareNoteRevisions(MUNICIPALITY_ID, noteId, sourceVersion, targetVersion);

		assertThat(result).isNotNull();
		assertThat(result.getOperations()).hasSize(1);
	}

	private ErrandEntity createErrandEntity() {
		final var randomBytes = new byte[30];
		new Random().nextBytes(randomBytes);

		return ErrandEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withId(ERRAND_ID)
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

	@Test
	@DisplayName("Verification that a removal takes the revisions with it a chunk at a time, reading only their ids, since a revision holds a full snapshot of the errand being removed")
	void deleteErrandRevisions() {
		when(revisionRepositoryMock.findIdsByNamespaceAndMunicipalityIdAndEntityId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(List.of(idProjection("first"), idProjection("second")));
		runChunksImmediately();

		service.deleteErrandRevisions(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		verify(chunkedDeleterMock).deleteInChunks(eq(List.of("first", "second")), any());
		verify(revisionRepositoryMock).deleteAllById(List.of("first", "second"));
		verify(revisionRepositoryMock, never()).findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(any(), any(), any());
		verifyNoInteractions(accessControlServiceMock);
	}

	/**
	 * Hands every chunk straight back to what the caller passed, so that a test sees the removal the deleter would have
	 * carried out.
	 */
	private void runChunksImmediately() {
		doAnswer(invocation -> {
			invocation.<Consumer<List<String>>>getArgument(1).accept(invocation.getArgument(0));
			return null;
		}).when(chunkedDeleterMock).deleteInChunks(anyList(), any());
	}

	private static IdProjection idProjection(final String id) {
		return new IdProjection() {

			@Override
			public String getId() {
				return id;
			}

			@Override
			public void setId(final String value) {
				// Nothing reads a value set here.
			}
		};
	}
}

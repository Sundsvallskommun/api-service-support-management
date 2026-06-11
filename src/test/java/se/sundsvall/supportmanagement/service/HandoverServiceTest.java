package se.sundsvall.supportmanagement.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverErrandRequest;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverInclude;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverMapping;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverSourceAction;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverSourceHandling;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverTarget;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.HandoverIdempotencyRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.HandoverIdempotencyEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;
import se.sundsvall.supportmanagement.integration.relation.RelationClient;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(MockitoExtension.class)
class HandoverServiceTest {

	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "source-errand-id";
	private static final String TARGET_NAMESPACE = "TARGET_NAMESPACE";
	private static final String TARGET_MUNICIPALITY_ID = "2282";
	private static final String NEW_ERRAND_ID = "new-errand-id";
	private static final String NEW_ERRAND_NUMBER = "KC-24010002";
	private static final String RELATION_ID = "relation-uuid";
	private static final String MAPPING_STATUS = "NEW_CASE";
	private static final String MAPPING_CATEGORY = "SUPPORT_CASE";
	private static final String MAPPING_TYPE = "OTHER_ISSUES";

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private NamespaceConfigService namespaceConfigServiceMock;

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private ErrandNoteService errandNoteServiceMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@Mock
	private RevisionService revisionServiceMock;

	@Mock
	private EventService eventServiceMock;

	@Mock
	private RelationClient relationClientMock;

	@Mock
	private HandoverIdempotencyRepository idempotencyRepositoryMock;

	@Mock
	private MetadataService metadataServiceMock;

	@InjectMocks
	private HandoverService service;

	private static ErrandEntity sourceEntity() {
		return ErrandEntity.create()
			.withId(ERRAND_ID)
			.withErrandNumber("KC-24010001")
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withTitle("Source title")
			.withStatus("OPEN")
			.withPriority("MEDIUM")
			.withResolution("OLD_RESOLUTION");
	}

	private static ErrandEntity targetEntity() {
		return ErrandEntity.create()
			.withId(NEW_ERRAND_ID)
			.withErrandNumber(NEW_ERRAND_NUMBER)
			.withNamespace(TARGET_NAMESPACE)
			.withMunicipalityId(TARGET_MUNICIPALITY_ID)
			.withPriority("MEDIUM")
			.withAttachments(new ArrayList<>());
	}

	private static HandoverErrandRequest minimalRequest() {
		return HandoverErrandRequest.create()
			.withTarget(HandoverTarget.create()
				.withNamespace(TARGET_NAMESPACE)
				.withMunicipalityId(TARGET_MUNICIPALITY_ID))
			.withMapping(HandoverMapping.create()
				.withStatus(MAPPING_STATUS)
				.withClassification(Classification.create().withCategory(MAPPING_CATEGORY).withType(MAPPING_TYPE))
				.withLabels(List.of()));
	}

	private void mockValidations() {
		when(metadataServiceMock.isValidated(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID, EntityType.STATUS)).thenReturn(true);
		when(metadataServiceMock.isValidated(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID, EntityType.CATEGORY)).thenReturn(true);
		when(metadataServiceMock.isValidated(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID, EntityType.TYPE)).thenReturn(true);
		when(metadataServiceMock.findStatuses(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any()))
			.thenReturn(List.of(Status.create().withName(MAPPING_STATUS)));
		when(metadataServiceMock.findCategories(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any()))
			.thenReturn(List.of(Category.create().withName(MAPPING_CATEGORY)));
		when(metadataServiceMock.findTypes(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID, MAPPING_CATEGORY))
			.thenReturn(List.of(Type.create().withName(MAPPING_TYPE)));
	}

	private void mockGoldenPath() {
		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(Optional.empty());
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false)).thenReturn(sourceEntity());
		mockValidations();
		when(errandServiceMock.createErrand(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any(), isNull())).thenReturn(NEW_ERRAND_ID);
		when(errandsRepositoryMock.findById(NEW_ERRAND_ID)).thenReturn(Optional.of(targetEntity()));
		when(relationClientMock.createRelation(eq(TARGET_MUNICIPALITY_ID), any()))
			.thenReturn(ResponseEntity.created(URI.create("/2282/relations/" + RELATION_ID)).build());
		when(revisionServiceMock.getLatestErrandRevision(any())).thenReturn(Revision.create());
	}

	@Test
	void handoverAlreadyPerformed() {
		final var existingRecord = HandoverIdempotencyEntity.create()
			.withSourceErrandId(ERRAND_ID)
			.withNewErrandId(NEW_ERRAND_ID)
			.withNewErrandNumber(NEW_ERRAND_NUMBER)
			.withTargetNamespace(TARGET_NAMESPACE)
			.withTargetMunicipalityId(TARGET_MUNICIPALITY_ID)
			.withRelationId(RELATION_ID)
			.withWarnings("");
		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID))
			.thenReturn(Optional.of(existingRecord));

		final var result = service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, minimalRequest());

		assertThat(result.getNewErrandId()).isEqualTo(NEW_ERRAND_ID);
		assertThat(result.getNewErrandNumber()).isEqualTo(NEW_ERRAND_NUMBER);
		assertThat(result.getRelationId()).isEqualTo(RELATION_ID);
		verify(accessControlServiceMock, never()).getErrand(anyString(), anyString(), anyString(), any(Boolean.class));
		verifyNoMoreInteractions(errandServiceMock, errandsRepositoryMock, relationClientMock);
	}

	@Test
	void handoverSavesRecord() {
		mockGoldenPath();

		final var result = service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, minimalRequest());

		assertThat(result.getNewErrandId()).isEqualTo(NEW_ERRAND_ID);
		assertThat(result.getNewErrandNumber()).isEqualTo(NEW_ERRAND_NUMBER);
		assertThat(result.getTarget().getNamespace()).isEqualTo(TARGET_NAMESPACE);
		assertThat(result.getTarget().getMunicipalityId()).isEqualTo(TARGET_MUNICIPALITY_ID);
		assertThat(result.getRelationId()).isEqualTo(RELATION_ID);

		final var captor = ArgumentCaptor.forClass(HandoverIdempotencyEntity.class);
		verify(idempotencyRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getSourceErrandId()).isEqualTo(ERRAND_ID);
		assertThat(captor.getValue().getNewErrandId()).isEqualTo(NEW_ERRAND_ID);
		assertThat(captor.getValue().getNewErrandNumber()).isEqualTo(NEW_ERRAND_NUMBER);
		assertThat(captor.getValue().getRelationId()).isEqualTo(RELATION_ID);
	}

	@Test
	void handoverValidateMappingsMissingStatus() {
		final var request = HandoverErrandRequest.create()
			.withTarget(HandoverTarget.create().withNamespace(TARGET_NAMESPACE).withMunicipalityId(TARGET_MUNICIPALITY_ID))
			.withMapping(HandoverMapping.create()
				.withStatus("")
				.withClassification(Classification.create().withCategory(MAPPING_CATEGORY).withType(MAPPING_TYPE))
				.withLabels(List.of()));
		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(Optional.empty());

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("status");
			});
	}

	@Test
	void handoverValidateMappingsStatusNotInTargetNamespace() {
		final var request = HandoverErrandRequest.create()
			.withTarget(HandoverTarget.create().withNamespace(TARGET_NAMESPACE).withMunicipalityId(TARGET_MUNICIPALITY_ID))
			.withMapping(HandoverMapping.create()
				.withStatus("UNKNOWN_STATUS")
				.withClassification(Classification.create().withCategory(MAPPING_CATEGORY).withType(MAPPING_TYPE))
				.withLabels(List.of()));
		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(Optional.empty());
		when(metadataServiceMock.isValidated(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID, EntityType.STATUS)).thenReturn(true);
		when(metadataServiceMock.findStatuses(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any())).thenReturn(List.of(Status.create().withName(MAPPING_STATUS)));

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("UNKNOWN_STATUS").contains(TARGET_NAMESPACE);
			});
	}

	@Test
	void handoverValidateMappingsMissingClassification() {
		final var request = HandoverErrandRequest.create()
			.withTarget(HandoverTarget.create().withNamespace(TARGET_NAMESPACE).withMunicipalityId(TARGET_MUNICIPALITY_ID))
			.withMapping(HandoverMapping.create()
				.withStatus(MAPPING_STATUS)
				.withClassification(null)
				.withLabels(List.of()));
		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(Optional.empty());
		when(metadataServiceMock.isValidated(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID, EntityType.STATUS)).thenReturn(true);
		when(metadataServiceMock.findStatuses(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any())).thenReturn(List.of(Status.create().withName(MAPPING_STATUS)));

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("classification");
			});
	}

	@Test
	void handoverValidateMappingsCategoryNotInTargetNamespace() {
		final var request = HandoverErrandRequest.create()
			.withTarget(HandoverTarget.create().withNamespace(TARGET_NAMESPACE).withMunicipalityId(TARGET_MUNICIPALITY_ID))
			.withMapping(HandoverMapping.create()
				.withStatus(MAPPING_STATUS)
				.withClassification(Classification.create().withCategory("UNKNOWN_CAT").withType(MAPPING_TYPE))
				.withLabels(List.of()));
		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(Optional.empty());
		when(metadataServiceMock.isValidated(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID, EntityType.STATUS)).thenReturn(true);
		when(metadataServiceMock.findStatuses(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any())).thenReturn(List.of(Status.create().withName(MAPPING_STATUS)));
		when(metadataServiceMock.isValidated(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID, EntityType.CATEGORY)).thenReturn(true);
		when(metadataServiceMock.findCategories(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any())).thenReturn(List.of(Category.create().withName(MAPPING_CATEGORY)));

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("UNKNOWN_CAT").contains(TARGET_NAMESPACE);
			});
	}

	@Test
	void handoverValidateMappingsTypeNotInTargetNamespace() {
		final var request = HandoverErrandRequest.create()
			.withTarget(HandoverTarget.create().withNamespace(TARGET_NAMESPACE).withMunicipalityId(TARGET_MUNICIPALITY_ID))
			.withMapping(HandoverMapping.create()
				.withStatus(MAPPING_STATUS)
				.withClassification(Classification.create().withCategory(MAPPING_CATEGORY).withType("UNKNOWN_TYPE"))
				.withLabels(List.of()));
		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(Optional.empty());
		when(metadataServiceMock.isValidated(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID, EntityType.STATUS)).thenReturn(true);
		when(metadataServiceMock.findStatuses(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any())).thenReturn(List.of(Status.create().withName(MAPPING_STATUS)));
		when(metadataServiceMock.isValidated(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID, EntityType.CATEGORY)).thenReturn(true);
		when(metadataServiceMock.findCategories(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any())).thenReturn(List.of(Category.create().withName(MAPPING_CATEGORY)));
		when(metadataServiceMock.isValidated(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID, EntityType.TYPE)).thenReturn(true);
		when(metadataServiceMock.findTypes(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID, MAPPING_CATEGORY)).thenReturn(List.of(Type.create().withName(MAPPING_TYPE)));

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("UNKNOWN_TYPE").contains(TARGET_NAMESPACE);
			});
	}

	@Test
	void handoverValidateMappingsMissingLabels() {
		final var request = HandoverErrandRequest.create()
			.withTarget(HandoverTarget.create().withNamespace(TARGET_NAMESPACE).withMunicipalityId(TARGET_MUNICIPALITY_ID))
			.withMapping(HandoverMapping.create()
				.withStatus(MAPPING_STATUS)
				.withClassification(Classification.create().withCategory(MAPPING_CATEGORY).withType(MAPPING_TYPE))
				.withLabels(null));
		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(Optional.empty());
		mockValidations();

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("labels");
			});
	}

	@Test
	void handoverValidateMappingsContactReasonNotInTargetNamespace() {
		final var request = HandoverErrandRequest.create()
			.withTarget(HandoverTarget.create().withNamespace(TARGET_NAMESPACE).withMunicipalityId(TARGET_MUNICIPALITY_ID))
			.withMapping(HandoverMapping.create()
				.withStatus(MAPPING_STATUS)
				.withClassification(Classification.create().withCategory(MAPPING_CATEGORY).withType(MAPPING_TYPE))
				.withLabels(List.of())
				.withContactReason("UNKNOWN_REASON"));
		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(Optional.empty());
		mockValidations();
		when(metadataServiceMock.isValidated(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID, EntityType.CONTACT_REASON)).thenReturn(true);
		when(metadataServiceMock.findContactReasons(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any())).thenReturn(List.of(ContactReason.create().withReason("OTHER_REASON")));

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("UNKNOWN_REASON").contains(TARGET_NAMESPACE);
			});
	}

	@Test
	void handoverRelationCreationFailureAbortsHandover() {
		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(Optional.empty());
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false)).thenReturn(sourceEntity());
		mockValidations();
		when(errandServiceMock.createErrand(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any(), isNull())).thenReturn(NEW_ERRAND_ID);
		when(errandsRepositoryMock.findById(NEW_ERRAND_ID)).thenReturn(Optional.of(targetEntity()));
		when(relationClientMock.createRelation(eq(TARGET_MUNICIPALITY_ID), any())).thenThrow(new RuntimeException("relation service down"));

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, minimalRequest()))
			.isInstanceOf(RuntimeException.class)
			.withMessageContaining("relation service down");
	}

	@Test
	void handoverWithCloseSourceHandling() {
		mockGoldenPath();
		final var request = minimalRequest()
			.withSourceHandling(HandoverSourceHandling.create()
				.withAction(HandoverSourceAction.CLOSE)
				.withStatus("SOLVED")
				.withResolution("HANDED_OVER"));

		service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request);

		final var errandCaptor = ArgumentCaptor.forClass(se.sundsvall.supportmanagement.api.model.errand.Errand.class);
		verify(errandServiceMock).updateErrand(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), errandCaptor.capture());
		assertThat(errandCaptor.getValue().getStatus()).isEqualTo("SOLVED");
		assertThat(errandCaptor.getValue().getResolution()).isEqualTo("HANDED_OVER");
	}

	@Test
	void handoverWithCloseSourceHandlingAndMissingStatusThrowsBadRequest() {
		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(Optional.empty());

		final var request = minimalRequest()
			.withSourceHandling(HandoverSourceHandling.create()
				.withAction(HandoverSourceAction.CLOSE));

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("status").contains("CLOSE");
			});
	}

	@Test
	void handoverWithSuspendSourceHandlingThrowsNotImplemented() {
		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(Optional.empty());

		final var request = minimalRequest()
			.withSourceHandling(HandoverSourceHandling.create()
				.withAction(HandoverSourceAction.SUSPEND));

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> assertThat(problem.getStatus()).isEqualTo(org.springframework.http.HttpStatus.NOT_IMPLEMENTED));
	}

	@Test
	void handoverEventLoggingFailureDoesNotAbortHandover() {
		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(Optional.empty());
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false)).thenReturn(sourceEntity());
		mockValidations();
		when(errandServiceMock.createErrand(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any(), isNull())).thenReturn(NEW_ERRAND_ID);
		when(errandsRepositoryMock.findById(NEW_ERRAND_ID)).thenReturn(Optional.of(targetEntity()));
		when(relationClientMock.createRelation(eq(TARGET_MUNICIPALITY_ID), any()))
			.thenReturn(ResponseEntity.created(URI.create("/2282/relations/" + RELATION_ID)).build());
		when(revisionServiceMock.getLatestErrandRevision(any())).thenThrow(new RuntimeException("revision service unavailable"));

		final var result = service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, minimalRequest());

		assertThat(result.getNewErrandId()).isEqualTo(NEW_ERRAND_ID);
		assertThat(result.getNewErrandNumber()).isEqualTo(NEW_ERRAND_NUMBER);
	}

	@Test
	void handoverWithIncludeAttachmentsSkipsNullFileData() {
		final var attachmentDataWithNullFile = AttachmentDataEntity.create();
		final var sourceAttachment = AttachmentEntity.create()
			.withFileName("document.pdf")
			.withMimeType("application/pdf")
			.withFileSize(1024)
			.withAttachmentData(attachmentDataWithNullFile);
		final var source = sourceEntity().withAttachments(new ArrayList<>(List.of(sourceAttachment)));
		final var target = targetEntity();

		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(Optional.empty());
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false)).thenReturn(source);
		when(errandsRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(source));
		mockValidations();
		when(errandServiceMock.createErrand(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any(), isNull())).thenReturn(NEW_ERRAND_ID);
		when(errandsRepositoryMock.findById(NEW_ERRAND_ID)).thenReturn(Optional.of(target));
		when(relationClientMock.createRelation(eq(TARGET_MUNICIPALITY_ID), any()))
			.thenReturn(ResponseEntity.created(URI.create("/2282/relations/" + RELATION_ID)).build());
		when(revisionServiceMock.getLatestErrandRevision(any())).thenReturn(Revision.create());

		final var request = minimalRequest()
			.withInclude(HandoverInclude.create().withAttachments(true));

		final var result = service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request);

		assertThat(result.getNewErrandId()).isEqualTo(NEW_ERRAND_ID);
		verify(attachmentRepositoryMock, never()).save(any());
	}

	@Test
	void handoverSelfHandoverThrowsBadRequest() {
		final var request = HandoverErrandRequest.create()
			.withTarget(HandoverTarget.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID))
			.withMapping(HandoverMapping.create()
				.withStatus(MAPPING_STATUS)
				.withClassification(Classification.create().withCategory(MAPPING_CATEGORY).withType(MAPPING_TYPE))
				.withLabels(List.of()));

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("differ");
			});
	}

	@Test
	void handoverTargetNamespaceNotConfiguredThrowsBadRequest() {
		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(Optional.empty());
		when(namespaceConfigServiceMock.get(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenThrow(new RuntimeException("not found"));

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, minimalRequest()))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains(TARGET_NAMESPACE).contains(TARGET_MUNICIPALITY_ID);
			});
	}

	@Test
	void handoverValidateMappingsLabelNotInTargetNamespace() {
		final var request = HandoverErrandRequest.create()
			.withTarget(HandoverTarget.create().withNamespace(TARGET_NAMESPACE).withMunicipalityId(TARGET_MUNICIPALITY_ID))
			.withMapping(HandoverMapping.create()
				.withStatus(MAPPING_STATUS)
				.withClassification(Classification.create().withCategory(MAPPING_CATEGORY).withType(MAPPING_TYPE))
				.withLabels(List.of("unknown-label-id")));
		when(idempotencyRepositoryMock.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(ERRAND_ID, TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(Optional.empty());
		mockValidations();
		when(metadataServiceMock.hasLabels(TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(true);
		when(metadataServiceMock.labelExistsById("unknown-label-id", TARGET_NAMESPACE, TARGET_MUNICIPALITY_ID)).thenReturn(false);

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("unknown-label-id").contains(TARGET_NAMESPACE);
			});
	}

	@Test
	void handoverWithCloseSourceHandlingAndClosingComment() {
		mockGoldenPath();
		final var request = minimalRequest()
			.withSourceHandling(HandoverSourceHandling.create()
				.withAction(HandoverSourceAction.CLOSE)
				.withStatus("SOLVED")
				.withClosingComment("Handed over to another namespace"));

		service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request);

		verify(errandNoteServiceMock).createErrandNote(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), any());
	}

}

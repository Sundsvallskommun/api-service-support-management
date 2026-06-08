package se.sundsvall.supportmanagement.service;

import java.net.URI;
import java.time.OffsetDateTime;
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
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.HandoverIdempotencyRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.HandoverIdempotencyEntity;
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
	private static final String IDEMPOTENCY_KEY = "my-idempotency-key";

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private NamespaceConfigService namespaceConfigServiceMock;

	@Mock
	private ErrandService errandServiceMock;

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
				.withStatus("NEW_CASE")
				.withClassification(Classification.create().withCategory("SUPPORT_CASE").withType("OTHER_ISSUES"))
				.withLabels(List.of()));
	}

	private void mockGoldenPath() {
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false)).thenReturn(sourceEntity());
		when(errandServiceMock.createErrand(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any(), isNull())).thenReturn(NEW_ERRAND_ID);
		when(errandsRepositoryMock.findById(NEW_ERRAND_ID)).thenReturn(Optional.of(targetEntity()));
		when(relationClientMock.createRelation(eq(TARGET_MUNICIPALITY_ID), any()))
			.thenReturn(ResponseEntity.created(URI.create("/2282/relations/" + RELATION_ID)).build());
		when(revisionServiceMock.getLatestErrandRevision(any())).thenReturn(Revision.create());
	}

	@Test
	void handoverWithIdempotencyKeyAlreadyCached() {
		final var cachedEntity = HandoverIdempotencyEntity.create()
			.withIdempotencyKey(IDEMPOTENCY_KEY)
			.withNewErrandId(NEW_ERRAND_ID)
			.withNewErrandNumber(NEW_ERRAND_NUMBER)
			.withTargetNamespace(TARGET_NAMESPACE)
			.withTargetMunicipalityId(TARGET_MUNICIPALITY_ID)
			.withRelationId(RELATION_ID)
			.withWarnings("")
			.withExpiresAt(OffsetDateTime.now().plusHours(1));
		when(idempotencyRepositoryMock.findByIdempotencyKeyAndExpiresAtAfter(eq(IDEMPOTENCY_KEY), any())).thenReturn(Optional.of(cachedEntity));

		final var result = service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, IDEMPOTENCY_KEY, minimalRequest());

		assertThat(result.getNewErrandId()).isEqualTo(NEW_ERRAND_ID);
		assertThat(result.getNewErrandNumber()).isEqualTo(NEW_ERRAND_NUMBER);
		assertThat(result.getRelationId()).isEqualTo(RELATION_ID);
		verify(accessControlServiceMock, never()).getErrand(anyString(), anyString(), anyString(), any(Boolean.class));
		verifyNoMoreInteractions(errandServiceMock, errandsRepositoryMock, relationClientMock);
	}

	@Test
	void handoverWithoutIdempotencyKey() {
		mockGoldenPath();

		final var result = service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, minimalRequest());

		assertThat(result.getNewErrandId()).isEqualTo(NEW_ERRAND_ID);
		assertThat(result.getNewErrandNumber()).isEqualTo(NEW_ERRAND_NUMBER);
		assertThat(result.getTarget().getNamespace()).isEqualTo(TARGET_NAMESPACE);
		assertThat(result.getTarget().getMunicipalityId()).isEqualTo(TARGET_MUNICIPALITY_ID);
		assertThat(result.getRelationId()).isEqualTo(RELATION_ID);
		verify(idempotencyRepositoryMock, never()).save(any());
	}

	@Test
	void handoverWithIdempotencyKeySavesRecord() {
		when(idempotencyRepositoryMock.findByIdempotencyKeyAndExpiresAtAfter(eq(IDEMPOTENCY_KEY), any())).thenReturn(Optional.empty());
		mockGoldenPath();

		service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, IDEMPOTENCY_KEY, minimalRequest());

		final var captor = ArgumentCaptor.forClass(HandoverIdempotencyEntity.class);
		verify(idempotencyRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
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
				.withClassification(Classification.create().withCategory("CAT").withType("TYPE"))
				.withLabels(List.of()));
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false)).thenReturn(sourceEntity());

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, request))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("status");
			});
	}

	@Test
	void handoverValidateMappingsMissingClassification() {
		final var request = HandoverErrandRequest.create()
			.withTarget(HandoverTarget.create().withNamespace(TARGET_NAMESPACE).withMunicipalityId(TARGET_MUNICIPALITY_ID))
			.withMapping(HandoverMapping.create()
				.withStatus("NEW_CASE")
				.withClassification(null)
				.withLabels(List.of()));
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false)).thenReturn(sourceEntity());

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, request))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("classification");
			});
	}

	@Test
	void handoverValidateMappingsMissingLabels() {
		final var request = HandoverErrandRequest.create()
			.withTarget(HandoverTarget.create().withNamespace(TARGET_NAMESPACE).withMunicipalityId(TARGET_MUNICIPALITY_ID))
			.withMapping(HandoverMapping.create()
				.withStatus("NEW_CASE")
				.withClassification(Classification.create().withCategory("CAT").withType("TYPE"))
				.withLabels(null));
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false)).thenReturn(sourceEntity());

		assertThatException()
			.isThrownBy(() -> service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, request))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("labels");
			});
	}

	@Test
	void handoverRelationCreationFailureDoesNotAbortHandover() {
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false)).thenReturn(sourceEntity());
		when(errandServiceMock.createErrand(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any(), isNull())).thenReturn(NEW_ERRAND_ID);
		when(errandsRepositoryMock.findById(NEW_ERRAND_ID)).thenReturn(Optional.of(targetEntity()));
		when(relationClientMock.createRelation(eq(TARGET_MUNICIPALITY_ID), any())).thenThrow(new RuntimeException("relation service down"));
		when(revisionServiceMock.getLatestErrandRevision(any())).thenReturn(Revision.create());

		final var result = service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, minimalRequest());

		assertThat(result.getNewErrandId()).isEqualTo(NEW_ERRAND_ID);
		assertThat(result.getRelationId()).isNull();
	}

	@Test
	void handoverWithCloseSourceHandling() {
		mockGoldenPath();
		final var request = minimalRequest()
			.withSourceHandling(HandoverSourceHandling.create()
				.withAction(HandoverSourceAction.CLOSE)
				.withResolution("HANDED_OVER"));

		service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, request);

		final var errandCaptor = ArgumentCaptor.forClass(se.sundsvall.supportmanagement.api.model.errand.Errand.class);
		verify(errandServiceMock).updateErrand(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), errandCaptor.capture());
		assertThat(errandCaptor.getValue().getResolution()).isEqualTo("HANDED_OVER");
	}

	@Test
	void handoverWithSuspendSourceHandlingLogsWarning() {
		mockGoldenPath();
		final var request = minimalRequest()
			.withSourceHandling(HandoverSourceHandling.create()
				.withAction(HandoverSourceAction.SUSPEND));

		service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, request);

		verify(errandServiceMock, never()).updateErrand(anyString(), anyString(), anyString(), any());
	}

	@Test
	void handoverEventLoggingFailureDoesNotAbortHandover() {
		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false)).thenReturn(sourceEntity());
		when(errandServiceMock.createErrand(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any(), isNull())).thenReturn(NEW_ERRAND_ID);
		when(errandsRepositoryMock.findById(NEW_ERRAND_ID)).thenReturn(Optional.of(targetEntity()));
		when(relationClientMock.createRelation(eq(TARGET_MUNICIPALITY_ID), any()))
			.thenReturn(ResponseEntity.created(URI.create("/2282/relations/" + RELATION_ID)).build());
		when(revisionServiceMock.getLatestErrandRevision(any())).thenThrow(new RuntimeException("revision service unavailable"));

		final var result = service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, minimalRequest());

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

		when(accessControlServiceMock.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false)).thenReturn(source);
		when(errandServiceMock.createErrand(eq(TARGET_NAMESPACE), eq(TARGET_MUNICIPALITY_ID), any(), isNull())).thenReturn(NEW_ERRAND_ID);
		when(errandsRepositoryMock.findById(NEW_ERRAND_ID)).thenReturn(Optional.of(target));
		when(relationClientMock.createRelation(eq(TARGET_MUNICIPALITY_ID), any()))
			.thenReturn(ResponseEntity.created(URI.create("/2282/relations/" + RELATION_ID)).build());
		when(revisionServiceMock.getLatestErrandRevision(any())).thenReturn(Revision.create());

		final var request = minimalRequest()
			.withInclude(HandoverInclude.create().withAttachments(true));

		final var result = service.handover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null, request);

		assertThat(result.getNewErrandId()).isEqualTo(NEW_ERRAND_ID);
		verify(attachmentRepositoryMock, never()).save(any());
	}

}

package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverErrandRequest;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverMapping;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverTarget;
import se.sundsvall.supportmanagement.api.model.handover.ClassificationMapping;
import se.sundsvall.supportmanagement.api.model.handover.ClassificationOption;
import se.sundsvall.supportmanagement.api.model.handover.ContactReasonMapping;
import se.sundsvall.supportmanagement.api.model.handover.DirectlyCopyable;
import se.sundsvall.supportmanagement.api.model.handover.HandoverPreview;
import se.sundsvall.supportmanagement.api.model.handover.HandoverPreviewRequest;
import se.sundsvall.supportmanagement.api.model.handover.LabelCandidate;
import se.sundsvall.supportmanagement.api.model.handover.LabelMapping;
import se.sundsvall.supportmanagement.api.model.handover.LabelMappingGroup;
import se.sundsvall.supportmanagement.api.model.handover.MappingRequired;
import se.sundsvall.supportmanagement.api.model.handover.MatchReason;
import se.sundsvall.supportmanagement.api.model.handover.MetadataOption;
import se.sundsvall.supportmanagement.api.model.handover.NotCopyable;
import se.sundsvall.supportmanagement.api.model.handover.StatusMapping;
import se.sundsvall.supportmanagement.api.model.handover.Warning;
import se.sundsvall.supportmanagement.api.model.handover.WarningType;
import se.sundsvall.supportmanagement.service.HandoverPreviewService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class HandoverResourceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();

	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/handover/preview";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private HandoverPreviewService serviceMock;

	private static HandoverPreviewRequest validRequest() {
		return HandoverPreviewRequest.create()
			.withTargetNamespace("OTHER_NAMESPACE")
			.withTargetMunicipalityId("2281");
	}

	private static HandoverPreview preview() {
		return HandoverPreview.create()
			.withDirectlyCopyable(DirectlyCopyable.create()
				.withTitle("title")
				.withPriority(Priority.HIGH)
				.withStakeholderCount(3)
				.withExternalTagCount(5)
				.withAttachmentCount(2))
			.withMappingRequired(MappingRequired.create()
				.withStatus(StatusMapping.create()
					.withSource(MetadataOption.create().withName("ONGOING").withDisplayName("Pågående"))
					.withSuggestedTarget("IN_PROGRESS")
					.withMatchReason(MatchReason.DISPLAY_NAME_EXACT)
					.withCandidates(List.of(MetadataOption.create().withName("IN_PROGRESS").withDisplayName("Pågående"))))
				.withClassification(ClassificationMapping.create()
					.withSource(ClassificationOption.create().withCategory("SUPPORT_CASE").withType("OTHER_ISSUES"))
					.withSuggestedCategory("SUPPORT_CASE")
					.withSuggestedType("OTHER_ISSUES")
					.withCandidates(Map.of("SUPPORT_CASE", List.of("OTHER_ISSUES"))))
				.withLabels(LabelMappingGroup.create()
					.withCandidates(List.of(LabelCandidate.create().withId("uuid-b").withDisplayName("Nyckelkort").withResourcePath("/access/keycard")))
					.withMappings(List.of(LabelMapping.create()
						.withSourceId("uuid-a")
						.withSourceDisplayName("Nyckelkort")
						.withSourceResourcePath("/access/keycard")
						.withSuggestedTargetId("uuid-b")
						.withMatchReason(MatchReason.RESOURCE_PATH_MATCH))))
				.withContactReason(ContactReasonMapping.create()
					.withSource("Bygglov")
					.withSuggested("Bygglov")
					.withCandidates(List.of("Bygglov"))))
			.withNotCopyable(List.of(NotCopyable.create().withField("phases").withReason("Phase history is source-specific")))
			.withWarnings(List.of(Warning.create().withType(WarningType.ROLE_NOT_IN_TARGET).withValue("EXTERNAL_REPORTER")));
	}

	@Test
	void handoverErrand() {
		final var request = HandoverErrandRequest.create()
			.withTarget(HandoverTarget.create().withNamespace("OTHER_NAMESPACE").withMunicipalityId("2281"))
			.withMapping(HandoverMapping.create().withStatus("NEW_CASE"));

		webTestClient.post()
			.uri(builder -> builder.path("/{municipalityId}/{namespace}/errands/{errandId}/handover")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().is5xxServerError();
	}

	@Test
	void previewHandover() {
		final var request = validRequest();
		final var expected = preview();

		when(serviceMock.previewHandover(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(HandoverPreviewRequest.class)))
			.thenReturn(expected);

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(HandoverPreview.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(expected);
		verify(serviceMock).previewHandover(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(HandoverPreviewRequest.class));
	}
}

package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import jakarta.servlet.http.HttpServletResponse;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.communication.Communication;
import se.sundsvall.supportmanagement.api.model.communication.EmailAttachment;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.service.CommunicationService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandCommunicationResourceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String COMMUNICATION_ID = randomUUID().toString();
	private static final String ATTACHMENT_ID = randomUUID().toString();
	private static final String PATH_PREFIX = "/{municipalityId}/{namespace}/errands/{errandId}/communication";
	private static final String PATH_SMS = "/sms";
	private static final String PATH_EMAIL = "/email";
	private static final String PATH_ATTACHMENTS = "/{communicationId}/attachments/{attachmentId}/streamed";

	@MockBean
	private CommunicationService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getCommunicationsOnErrand() {

		// Mock
		when(serviceMock.readCommunications(anyString(), anyString(), anyString()))
			.thenReturn(List.of(Communication.create()));

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_PREFIX)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Communication.class)
			.returnResult();

		// Verification
		assertThat(response).isNotNull();
		assertThat(response.getResponseBody()).isNotNull().hasSize(1);
		verify(serviceMock).readCommunications(anyString(), anyString(), anyString());
	}

	@Test
	void setViewedStatusForCommunication() {
		// Parameter values
		final var messageID = randomUUID().toString();
		final var isViewed = true;

		// Call
		webTestClient.put()
			.uri(builder -> builder.path(PATH_PREFIX + "/{messageID}/viewed/{isViewed}")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "messageID", messageID, "isViewed", isViewed)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		// Verification
		verify(serviceMock).updateViewedStatus(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, messageID, isViewed);
	}

	@Test
	void sendSms() {

		// Parameter values
		final var requestBody = smsRequest();

		// Call
		webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_SMS)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		// Verification
		verify(serviceMock).sendSms(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, requestBody);
		verify(serviceMock, never()).sendEmail(any(), any(), any(), any());
	}

	@Test
	void sendEmail() {

		// Parameter values
		final var requestBody = emailRequest(false);

		webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		// Verification
		verify(serviceMock).sendEmail(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, requestBody);
		verify(serviceMock, never()).sendSms(any(), any(), any(), any());
	}

	@Test
	void sendEmailWithAttachment() {

		// Parameter values
		final var requestBody = emailRequest(true);

		webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		// Verification
		verify(serviceMock).sendEmail(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, requestBody);
		verify(serviceMock, never()).sendSms(any(), any(), any(), any());
	}

	@Test
	void getMessageAttachmentStreamed() {

		// ACT
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH_PREFIX + PATH_ATTACHMENTS)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "communicationId", COMMUNICATION_ID, "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.returnResult();

		verify(serviceMock).getMessageAttachmentStreamed(any(String.class), any(String.class), any(String.class), any(HttpServletResponse.class));
	}

	private static SmsRequest smsRequest() {
		return SmsRequest.create()
			.withMessage("message")
			.withRecipient("+46701234567")
			.withSender("sender");
	}

	private static EmailRequest emailRequest(boolean withAttachment) {
		return EmailRequest.create()
			.withHtmlMessage("htmlMessage")
			.withMessage("message")
			.withRecipient("recipient@recipient.com")
			.withSender("sender@sender.com")
			.withSubject("subject")
			.withAttachments(withAttachment ? List.of(attachment()) : null);
	}

	private static EmailAttachment attachment() {
		return EmailAttachment.create()
			.withBase64EncodedString("aGVsbG8gd29ybGQK")
			.withName("name");
	}
}

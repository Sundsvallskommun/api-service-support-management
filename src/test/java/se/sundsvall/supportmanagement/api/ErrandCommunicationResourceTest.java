package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.communication.Communication;
import se.sundsvall.supportmanagement.api.model.communication.EmailAttachment;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.api.model.communication.WebMessageAttachment;
import se.sundsvall.supportmanagement.api.model.communication.WebMessageRequest;
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
	private static final String PATH_WEB_MESSAGE = "/webmessage";
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
		verifyNoMoreInteractions(serviceMock);
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
		verifyNoMoreInteractions(serviceMock);
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
		verifyNoMoreInteractions(serviceMock);
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void sendEmail(boolean withAttachments) {

		// Parameter values
		final var requestBody = emailRequest(withAttachments);

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
		verifyNoMoreInteractions(serviceMock);
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void sendWebMessage(boolean withAttachment) {

		// Parameter values
		final var requestBody = webMessageRequest(withAttachment);

		webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_WEB_MESSAGE)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		// Verification
		verify(serviceMock).sendWebMessage(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, requestBody);
		verifyNoMoreInteractions(serviceMock);
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

		verify(serviceMock).getMessageAttachmentStreamed(any(String.class), any(String.class), any(String.class), any(String.class), any(String.class), any(HttpServletResponse.class));
		verifyNoMoreInteractions(serviceMock);
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

	private static WebMessageRequest webMessageRequest(boolean withAttachment) {
		return WebMessageRequest.create()
			.withMessage("message")
			.withAttachmentIds(List.of("1", "2"))
			.withAttachments(withAttachment ? List.of(WebMessageAttachment.create().withName("attachmentName").withBase64EncodedString("ZGF0YQ==")) : null);
	}

	private static EmailAttachment attachment() {
		return EmailAttachment.create()
			.withBase64EncodedString("aGVsbG8gd29ybGQK")
			.withName("name");
	}
}

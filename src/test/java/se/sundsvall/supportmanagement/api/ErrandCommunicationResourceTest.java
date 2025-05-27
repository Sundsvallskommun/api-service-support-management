package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.web.reactive.function.BodyInserters.fromMultipartData;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.communication.Communication;
import se.sundsvall.supportmanagement.api.model.communication.EmailAttachment;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.api.model.communication.WebMessageAttachment;
import se.sundsvall.supportmanagement.api.model.communication.WebMessageRequest;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Conversation;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationRequest;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationType;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Identifier;
import se.sundsvall.supportmanagement.api.model.communication.conversation.KeyValues;
import se.sundsvall.supportmanagement.api.model.communication.conversation.MessageRequest;
import se.sundsvall.supportmanagement.service.CommunicationService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandCommunicationResourceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String COMMUNICATION_ID = randomUUID().toString();
	private static final String ATTACHMENT_ID = randomUUID().toString();
	private static final String CONVERSATION_ID = randomUUID().toString();
	private static final String PATH_PREFIX = "/{municipalityId}/{namespace}/errands/{errandId}/communication";
	private static final String PATH_SMS = "/sms";
	private static final String PATH_EMAIL = "/email";
	private static final String PATH_WEB_MESSAGE = "/webmessage";
	private static final String PATH_ATTACHMENTS = "/{communicationId}/attachments/{attachmentId}";
	private static final String PATH_CONVERSATIONS = "/conversations";

	@MockitoBean
	private CommunicationService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static SmsRequest smsRequest() {
		return SmsRequest.create()
			.withMessage("message")
			.withRecipient("+46701234567")
			.withSender("sender");
	}

	private static EmailRequest emailRequest(final boolean withAttachment) {
		return EmailRequest.create()
			.withHtmlMessage("htmlMessage")
			.withMessage("message")
			.withRecipient("recipient@recipient.com")
			.withSender("sender@sender.com")
			.withSubject("subject")
			.withAttachments(withAttachment ? List.of(attachment()) : null);
	}

	private static WebMessageRequest webMessageRequest(final boolean withAttachment) {
		return WebMessageRequest.create()
			.withMessage("message")
			.withAttachmentIds(List.of("1", "2"))
			.withAttachments(withAttachment ? List.of(WebMessageAttachment.create().withFileName("attachmentName").withBase64EncodedString("ZGF0YQ==")) : null);
	}

	private static EmailAttachment attachment() {
		return EmailAttachment.create()
			.withBase64EncodedString("aGVsbG8gd29ybGQK")
			.withFileName("name");
	}

	private static ConversationRequest conversationRequest() {
		return ConversationRequest.create()
			.withExternalReferences(List.of(KeyValues.create()
				.withKey("theKey")
				.withValues(List.of("externalReferenceValue"))))
			.withMetadata(List.of(KeyValues.create()
				.withKey("theMetadata")
				.withValues(List.of("metadataValue"))))
			.withParticipants(List.of(Identifier.create()
				.withType("adAccount")
				.withValue("joe01doe")))
			.withTopic("The topic")
			.withType(ConversationType.INTERNAL);
	}

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
	void sendEmail(final boolean withAttachments) {

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
	void sendWebMessage(final boolean withAttachment) {

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
	void getMessageAttachment() {

		// ACT
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH_PREFIX + PATH_ATTACHMENTS)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "communicationId", COMMUNICATION_ID, "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.returnResult();

		// Assert
		verify(serviceMock).getMessageAttachmentStreamed(any(String.class), any(String.class), any(String.class), any(String.class), any(String.class), any(HttpServletResponse.class));
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void createConversation() {

		final var request = conversationRequest();

		// Mock
		// TODO: Mock service.

		// Call
		webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.accept(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/communication/conversations/0")
			.expectHeader().contentType(ALL_VALUE)
			.expectBody().isEmpty();

		// Verification

		// TODO: Verification of service call.

		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void updateConversation() {

		// Arrange
		final var conversationId = randomUUID().toString();
		final var request = conversationRequest();

		// Mock
		// TODO: Mock service.

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS + "/{conversationId}")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "conversationId", conversationId)))
			.accept(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Conversation.class)
			.returnResult();

		// Verification
		assertThat(response).isNotNull();
		assertThat(response.getResponseBody()).isNotNull();
		// TODO: Verification of service call.

		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void getConversations() {

		// Mock
		// TODO: Mock service.

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Conversation.class)
			.returnResult();

		// Verification
		assertThat(response).isNotNull();
		assertThat(response.getResponseBody()).isNotNull().hasSize(1);

		// TODO: Verification of service call.

		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void getConversationById() {

		// Arrange
		final var conversationId = randomUUID().toString();

		// Mock
		// TODO: Mock service.

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS + "/{conversationId}")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "conversationId", conversationId)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Conversation.class)
			.returnResult();

		// Verification
		assertThat(response).isNotNull();

		// TODO: Verification of service call.

		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void createConversationMessage() {

		// Arrange
		final var messageRequest = MessageRequest.create()
			.withContent("content");

		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("attachments", "file-content").filename("test1.txt").contentType(TEXT_PLAIN);
		multipartBodyBuilder.part("attachments", "file-content").filename("tesst2.txt").contentType(TEXT_PLAIN);
		multipartBodyBuilder.part("requestBody", messageRequest);

		// TODO: Mock service.

		// Act
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS + "/{conversationId}/messages")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "conversationId", CONVERSATION_ID)))
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().contentType(ALL_VALUE)
			.expectBody().isEmpty();

		// Assert
		assertThat(response).isNotNull();

		// TODO: Verification of service call.

		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void getConversationMessages() {

		// Mock
		// TODO: Mock service.

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS + "/{conversationId}/messages")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "conversationId", CONVERSATION_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Conversation.class)
			.returnResult();

		// Verification
		assertThat(response).isNotNull();
		assertThat(response.getResponseBody()).isNotNull().hasSize(1);

		// TODO: Verification of service call.

		verifyNoMoreInteractions(serviceMock);
	}
}

package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.web.reactive.function.BodyInserters.fromMultipartData;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.TOO_MANY_REQUESTS;

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
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.communication.EmailAttachment;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.api.model.communication.WebMessageAttachment;
import se.sundsvall.supportmanagement.api.model.communication.WebMessageRequest;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationRequest;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationType;
import se.sundsvall.supportmanagement.api.model.communication.conversation.MessageRequest;
import se.sundsvall.supportmanagement.service.CommunicationService;
import se.sundsvall.supportmanagement.service.ConversationService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandCommunicationResourceFailureTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String MESSAGE_ID = randomUUID().toString();
	private static final String CONVERSATION_ID = randomUUID().toString();
	private static final boolean IS_VIEWED = true;
	private static final String INVALID = "#invalid#";
	private static final String CONSTRAINT_VIOLATION = "Constraint Violation";
	private static final String PATH_PREFIX = "/{municipalityId}/{namespace}/errands/{errandId}/communication";
	private static final String PATH_SMS = "/sms";
	private static final String PATH_EMAIL = "/email";
	private static final String PATH_WEB_MESSAGE = "/webmessage";
	private static final String PATH_CONVERSATIONS = "/conversations";
	private static final String PATH_ATTACHMENTS = "/{communicationId}/attachments/{attachmentId}";

	@MockitoBean
	private CommunicationService communicationServiceMock;

	@MockitoBean
	private ConversationService conversationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static SmsRequest smsRequest() {
		return SmsRequest.create()
			.withMessage("message")
			.withRecipient("+46701234567")
			.withSender("sender");
	}

	private static EmailRequest emailRequest() {
		return EmailRequest.create()
			.withHtmlMessage("htmlMessage")
			.withMessage("message")
			.withRecipient("recipient@recipient.com")
			.withSender("sender@sender.com")
			.withSubject("subject");
	}

	private static WebMessageRequest webMessageRequest() {
		return WebMessageRequest.create()
			.withMessage("message");
	}

	@Test
	void getMessagesOnErrandWithInvalidNamespace() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_PREFIX).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("getCommunications.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void getMessagesOnErrandWithInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_PREFIX).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("getCommunications.municipalityId", "not a valid municipality ID"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);

	}

	@Test
	void getMessagesOnErrandWithInvalidErrandId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_PREFIX).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", INVALID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("getCommunications.errandId", "not a valid UUID"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void setViewedStatusForMessageWithInvalidNamespace() {

		// Call
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PATH_PREFIX + "/{messageID}/viewed/{isViewed}").build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "messageID", MESSAGE_ID, "isViewed", IS_VIEWED)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("updateViewedStatus.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void setViewedStatusForMessageWithInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PATH_PREFIX + "/{messageID}/viewed/{isViewed}").build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID, "messageID", MESSAGE_ID, "isViewed", IS_VIEWED)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("updateViewedStatus.municipalityId", "not a valid municipality ID"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void setViewedStatusForMessageWithInvalidErrandId() {

		// Call
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PATH_PREFIX + "/{messageID}/viewed/{isViewed}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", INVALID, "messageID", MESSAGE_ID, "isViewed", IS_VIEWED)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("updateViewedStatus.errandId", "not a valid UUID"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void setViewedStatusForMessageWithInvalidMessageId() {

		// Call
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PATH_PREFIX + "/{messageID}/viewed/{isViewed}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "messageID", INVALID, "isViewed", IS_VIEWED)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("updateViewedStatus.communicationId", "not a valid UUID"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void setViewedStatusForMessageWithInvalidIsViewed() {

		// Call
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PATH_PREFIX + "/{messageID}/viewed/{isViewed}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "messageID", MESSAGE_ID, "isViewed", INVALID)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(AbstractThrowableProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Bad Request");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("Method parameter 'isViewed': Failed to convert value of type 'java.lang.String' to required type 'boolean'; Invalid boolean value [" + INVALID + "]");

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		PATH_SMS, PATH_EMAIL, PATH_WEB_MESSAGE
	})
	void sendNotificationWithInvalidNamespace(final String type) {

		String field = null;
		switch (type) {
			case PATH_SMS -> field = "sendSms.namespace";
			case PATH_EMAIL -> field = "sendEmail.namespace";
			case PATH_WEB_MESSAGE -> field = "sendWebMessage.namespace";
			default -> throw new IllegalArgumentException("Missing mapping for " + type);
		}

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + type).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.bodyValue(PATH_SMS.equals(type) ? smsRequest() : emailRequest())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple(field, "can only contain A-Z, a-z, 0-9, - and _"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		PATH_SMS, PATH_EMAIL, PATH_WEB_MESSAGE
	})
	void sendNotificationWithInvalidMunicipalityId(final String type) {

		String field = null;
		switch (type) {
			case PATH_SMS -> field = "sendSms.municipalityId";
			case PATH_EMAIL -> field = "sendEmail.municipalityId";
			case PATH_WEB_MESSAGE -> field = "sendWebMessage.municipalityId";
			default -> throw new IllegalArgumentException("Missing mapping for " + type);
		}

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + type).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(PATH_SMS.equals(type) ? smsRequest() : emailRequest())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo(CONSTRAINT_VIOLATION);
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple(field, "not a valid municipality ID"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		PATH_SMS, PATH_EMAIL, PATH_WEB_MESSAGE
	})
	void sendNotificationWithInvalidErrandId(final String type) {

		String field = null;
		switch (type) {
			case PATH_SMS -> field = "sendSms.errandId";
			case PATH_EMAIL -> field = "sendEmail.errandId";
			case PATH_WEB_MESSAGE -> field = "sendWebMessage.errandId";
			default -> throw new IllegalArgumentException("Missing mapping for " + type);
		}

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + type).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", INVALID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(PATH_SMS.equals(type) ? smsRequest() : emailRequest())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo(CONSTRAINT_VIOLATION);
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple(field, "not a valid UUID"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void sendSmsWithoutBody() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_SMS).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("""
			Required request body is missing: org.springframework.http.ResponseEntity<java.lang.Void> \
			se.sundsvall.supportmanagement.api.ErrandCommunicationResource.sendSms(java.lang.String,java.lang.String,\
			java.lang.String,se.sundsvall.supportmanagement.api.model.communication.SmsRequest)""");

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void sendSmsWithEmptyBody() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_SMS).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(SmsRequest.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo(CONSTRAINT_VIOLATION);
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("message", "must not be blank"),
			tuple("recipient", "must be a valid MSISDN (example: +46701234567). Regular expression: ^\\+[1-9][\\d]{3,14}$"),
			tuple("sender", "must not be null"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void sendSmsWithInvalidValues() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_SMS).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(smsRequest()
				.withRecipient("123")
				.withSender("12_char_name"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo(CONSTRAINT_VIOLATION);
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("recipient", "must be a valid MSISDN (example: +46701234567). Regular expression: ^\\+[1-9][\\d]{3,14}$"),
			tuple("sender", "size must be between 1 and 11"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void sendEmailWithoutBody() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("""
			Required request body is missing: org.springframework.http.ResponseEntity<java.lang.Void> \
			se.sundsvall.supportmanagement.api.ErrandCommunicationResource.sendEmail(java.lang.String,java.lang.String,\
			java.lang.String,se.sundsvall.supportmanagement.api.model.communication.EmailRequest)""");

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void sendEmailWithEmptyBody() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(EmailRequest.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo(CONSTRAINT_VIOLATION);
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("htmlMessage", "must not be blank"),
			tuple("message", "must not be blank"),
			tuple("recipient", "must not be null"),
			tuple("sender", "must not be null"),
			tuple("subject", "must not be blank"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void sendEmailWithInvalidValues() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(emailRequest()
				.withRecipient("not_an_email")
				.withSender("not_an_email"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo(CONSTRAINT_VIOLATION);
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("recipient", "must be a well-formed email address"),
			tuple("sender", "must be a well-formed email address"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void sendWebMessageWithEmptyBody() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_WEB_MESSAGE).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(WebMessageRequest.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo(CONSTRAINT_VIOLATION);
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("message", "must not be blank"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void sendWebMessageWithoutBody() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_WEB_MESSAGE).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("""
			Required request body is missing: org.springframework.http.ResponseEntity<java.lang.Void> \
			se.sundsvall.supportmanagement.api.ErrandCommunicationResource.sendWebMessage(java.lang.String,java.lang.String,\
			java.lang.String,se.sundsvall.supportmanagement.api.model.communication.WebMessageRequest)""");

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		PATH_EMAIL, PATH_WEB_MESSAGE
	})
	void sendWithEmptyAttachment(final String path) {

		Object body = null;
		switch (path) {
			case PATH_EMAIL -> body = emailRequest().withAttachments(List.of(EmailAttachment.create()));
			case PATH_WEB_MESSAGE -> body = webMessageRequest().withAttachments(List.of(WebMessageAttachment.create()));
			default -> throw new IllegalArgumentException("Missing mapping for " + path);
		}

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + path).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo(CONSTRAINT_VIOLATION);
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("attachments[0].base64EncodedString", "not a valid BASE64-encoded string"),
			tuple("attachments[0].fileName", "must not be blank"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		PATH_EMAIL, PATH_WEB_MESSAGE
	})
	void sendWithInvalidAttachmentString(final String path) {

		final var name = "name";
		final var data = "data:image/png;base64,iVBOR";
		Object body = null;
		switch (path) {
			case PATH_EMAIL -> body = emailRequest().withAttachments(List.of(EmailAttachment.create()
				.withFileName(name)
				.withBase64EncodedString(data)));
			case PATH_WEB_MESSAGE -> body = webMessageRequest().withAttachments(List.of(WebMessageAttachment.create()
				.withFileName(name)
				.withBase64EncodedString(data)));
			default -> throw new IllegalArgumentException("Missing mapping for " + path);
		}

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + path).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo(CONSTRAINT_VIOLATION);
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("attachments[0].base64EncodedString", "not a valid BASE64-encoded string"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void getMessageAttachmentServiceBusy() {

		// Arrange
		final var attachmentId = randomUUID().toString();
		final var communicationId = randomUUID().toString();

		doThrow(Problem.valueOf(TOO_MANY_REQUESTS, "Service is currently unavailable, please try again later."))
			.when(communicationServiceMock).getMessageAttachmentStreamed(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(communicationId), eq(attachmentId), any(HttpServletResponse.class));

		// Act
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH_PREFIX + PATH_ATTACHMENTS)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "communicationId", communicationId, "attachmentId", attachmentId)))
			.exchange()
			.expectStatus().is4xxClientError()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Too Many Requests");
		assertThat(response.getStatus()).isEqualTo(TOO_MANY_REQUESTS);
		assertThat(response.getDetail()).isEqualTo("Service is currently unavailable, please try again later.");

		// Assert
		verify(communicationServiceMock).getMessageAttachmentStreamed(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(communicationId), eq(attachmentId), any(HttpServletResponse.class));
		verifyNoMoreInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void createConversationWithEmptyRequestBody() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo(BAD_REQUEST.getReasonPhrase());
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo(
			"Required request body is missing: org.springframework.http.ResponseEntity<java.lang.Void> se.sundsvall.supportmanagement.api.ErrandCommunicationResource.createConversation(java.lang.String,java.lang.String,java.lang.String,se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationRequest)");

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void createConversationWithNoType() {

		// Arrange
		final var request = ConversationRequest.create()
			.withType(null)
			.withTopic("The topic");

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.accept(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("type", "must not be null"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void createConversationWithBlankTopic() {

		// Arrange
		final var request = ConversationRequest.create()
			.withType(ConversationType.EXTERNAL)
			.withTopic(" ");

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.accept(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("topic", "must not be blank"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void getConversationsWithInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("getConversations.municipalityId", "not a valid municipality ID"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void getConversationsWithInvalidNamespace() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("getConversations.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void getConversationByIdWithInvalidConversationId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS + "/{conversationId}")
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "conversationId", INVALID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("getConversation.conversationId", "not a valid UUID"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void updateConversationWithInvalidConversationId() {

		// Arrange
		final var request = ConversationRequest.create()
			.withType(ConversationType.EXTERNAL)
			.withTopic("The topic");

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS + "/{conversationId}")
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "conversationId", INVALID)))
			.accept(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("updateConversation.conversationId", "not a valid UUID"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void updateConversationWithInvalidBody() {

		// Arrange
		final var request = ConversationRequest.create();

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS + "/{conversationId}")
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "conversationId", CONVERSATION_ID)))
			.accept(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(
				tuple("topic", "must not be blank"),
				tuple("type", "must not be null"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void updateConversationsEmptyRequestBody() {

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS + "/{conversationId}")
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "conversationId", CONVERSATION_ID)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo(BAD_REQUEST.getReasonPhrase());
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo(
			"Required request body is missing: org.springframework.http.ResponseEntity<se.sundsvall.supportmanagement.api.model.communication.conversation.Conversation> se.sundsvall.supportmanagement.api.ErrandCommunicationResource.updateConversation(java.lang.String,java.lang.String,java.lang.String,java.lang.String,se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationRequest)");

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void createConversationMessageEmptyRequestBody() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS + "/{conversationId}/messages")
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "conversationId", CONVERSATION_ID)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(new MultipartBodyBuilder().build())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo(BAD_REQUEST.getReasonPhrase());
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("Required part 'message' is not present.");

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void createConversationMessageInvalidMessageAttributes() {

		// Arrange
		final var messageRequest = MessageRequest.create()
			.withContent(" ")
			.withInReplyToMessageId("invalid");

		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("message", messageRequest).contentType(APPLICATION_JSON);

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS + "/{conversationId}/messages")
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "conversationId", CONVERSATION_ID)))
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(
				tuple("content", "must not be blank"),
				tuple("inReplyToMessageId", "not a valid UUID"));

		// Verification
		verifyNoInteractions(communicationServiceMock, conversationServiceMock);
	}

	@Test
	void getConversationMessageAttachmentInvalidAttachmentId() {

		// Arrange
		final var attachmentId = "invalid";

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_CONVERSATIONS + "/{conversationId}/messages/{messageId}/attachments/{attachmentId}")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "conversationId", CONVERSATION_ID, "messageId", MESSAGE_ID, "attachmentId", attachmentId)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("getConversationMessageAttachment.attachmentId", "not a valid UUID"));

		// Assert
		verifyNoMoreInteractions(communicationServiceMock, conversationServiceMock);
	}
}

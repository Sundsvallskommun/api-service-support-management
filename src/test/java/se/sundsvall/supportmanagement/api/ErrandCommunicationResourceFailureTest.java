package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

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
import org.springframework.util.StringUtils;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.communication.EmailAttachment;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.service.CommunicationService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandCommunicationResourceFailureTest {

	private static final String NAMESPACE = "name.space";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String ERRAND_ID = randomUUID().toString();

	private static final String MESSAGE_ID = randomUUID().toString();

	private static final boolean IS_VIEWED = true;

	private static final String INVALID = "#invalid#";

	private static final String CONSTRAINT_VIOLATION = "Constraint Violation";

	private static final String PATH_PREFIX = "/{municipalityId}/{namespace}/errands/{id}/communication";

	private static final String PATH_SMS = "/sms";

	private static final String PATH_EMAIL = "/email";

	@MockBean
	private CommunicationService serviceMock;

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

	@Test
	void getMessagesOnErrandWithInvalidNamespace() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_PREFIX).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
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
			.containsExactly(tuple("getCommunications.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// Verification
		verifyNoInteractions(serviceMock);
	}

	@Test
	void getMessagesOnErrandWithInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_PREFIX).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", ERRAND_ID)))
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
		verifyNoInteractions(serviceMock);

	}

	@Test
	void getMessagesOnErrandWithInvalidErrandId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_PREFIX).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID)))
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
		verifyNoInteractions(serviceMock);

	}

	@Test
	void setViewedStatusForMessageWithInvalidNamespace() {

		// Call
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PATH_PREFIX + "/{messageID}/viewed/{isViewed}").build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "messageID", MESSAGE_ID, "isViewed", IS_VIEWED)))
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
			.containsExactly(tuple("updateViewedStatus.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// Verification
		verifyNoInteractions(serviceMock);
	}

	@Test
	void setViewedStatusForMessageWithInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PATH_PREFIX + "/{messageID}/viewed/{isViewed}").build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", ERRAND_ID, "messageID", MESSAGE_ID, "isViewed", IS_VIEWED)))
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
		verifyNoInteractions(serviceMock);
	}

	@Test
	void setViewedStatusForMessageWithInvalidErrandId() {

		// Call
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PATH_PREFIX + "/{messageID}/viewed/{isViewed}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID, "messageID", MESSAGE_ID, "isViewed", IS_VIEWED)))
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
		verifyNoInteractions(serviceMock);
	}

	@Test
	void setViewedStatusForMessageWithInvalidMessageId() {

		// Call
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PATH_PREFIX + "/{messageID}/viewed/{isViewed}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "messageID", INVALID, "isViewed", IS_VIEWED)))
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
			.containsExactly(tuple("updateViewedStatus.communicationID", "not a valid UUID"));

		// Verification
		verifyNoInteractions(serviceMock);
	}

	@Test
	void setViewedStatusForMessageWithInvalidIsViewed() {

		// Call
		final var response = webTestClient.put()
			.uri(builder -> builder.path(PATH_PREFIX + "/{messageID}/viewed/{isViewed}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "messageID", MESSAGE_ID, "isViewed", INVALID)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(AbstractThrowableProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Bad Request");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("Failed to convert value of type 'java.lang.String' to required type 'boolean'; Invalid boolean value [" + INVALID + "]");
		// Verification
		verifyNoInteractions(serviceMock);
	}

	@ParameterizedTest
	@ValueSource(strings = {PATH_SMS, PATH_EMAIL})
	void sendNotificationWithInvalidNamespace(final String type) {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + type).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
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
			.containsExactly(tuple("send" + StringUtils.capitalize(type.substring(1)) + ".namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// Verification
		verifyNoInteractions(serviceMock);
	}

	@ParameterizedTest
	@ValueSource(strings = {PATH_SMS, PATH_EMAIL})
	void sendNotificationWithInvalidMunicipalityId(final String type) {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + type).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", ERRAND_ID)))
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
			.containsExactly(tuple("send" + StringUtils.capitalize(type.substring(1)) + ".municipalityId", "not a valid municipality ID"));

		// Verification
		verifyNoInteractions(serviceMock);
	}

	@ParameterizedTest
	@ValueSource(strings = {PATH_SMS, PATH_EMAIL})
	void sendNotificationWithInvalidErrandId(final String type) {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + type).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID)))
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
			.containsExactly(tuple("send" + StringUtils.capitalize(type.substring(1)) + ".errandId", "not a valid UUID"));

		// Verification
		verifyNoInteractions(serviceMock);
	}

	@Test
	void sendSmsWithoutBody() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_SMS).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
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
		verifyNoInteractions(serviceMock);
	}

	@Test
	void sendSmsWithEmptyBody() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_SMS).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
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
		verifyNoInteractions(serviceMock);
	}

	@Test
	void sendSmsWithInvalidValues() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_SMS).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
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
		verifyNoInteractions(serviceMock);
	}

	@Test
	void sendEmailWithoutBody() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
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
		verifyNoInteractions(serviceMock);
	}

	@Test
	void sendEmailWithEmptyBody() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
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
		verifyNoInteractions(serviceMock);
	}

	@Test
	void sendEmailWithInvalidValues() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
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
		verifyNoInteractions(serviceMock);
	}

	@Test
	void sendEmailWithEmptyAttachment() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(emailRequest()
				.withAttachments(List.of(EmailAttachment.create())))
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
			tuple("attachments[0].name", "must not be blank"));

		// Verification
		verifyNoInteractions(serviceMock);
	}

	@Test
	void sendEmailWithInvalidAttachmentString() {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(emailRequest()
				.withAttachments(List.of(EmailAttachment.create()
					.withName("name")
					.withBase64EncodedString("data:image/png;base64,iVBOR"))))
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
		verifyNoInteractions(serviceMock);
	}

}

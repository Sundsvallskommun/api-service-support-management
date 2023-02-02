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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
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

	private static final String ERRAND_ID = randomUUID().toString();
	private static final String CONSTRAINT_VIOLATION = "Constraint Violation";
	private static final String PATH_PREFIX = "/errands/{id}/communication";
	private static final String PATH_SMS = "/sms";
	private static final String PATH_EMAIL = "/email";

	@MockBean
	private CommunicationService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void sendSmsWithoutBody() {

		// Call
		final var response = webTestClient.post().uri(builder -> builder.path(PATH_PREFIX + PATH_SMS).build(Map.of("id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("""
			Required request body is missing: public org.springframework.http.ResponseEntity<java.lang.Void>\s\
			se.sundsvall.supportmanagement.api.ErrandCommunicationResource.sendSms(java.lang.String,\
			se.sundsvall.supportmanagement.api.model.communication.SmsRequest)""");

		// Verification
		verifyNoInteractions(serviceMock);
	}

	@Test
	void sendSmsWithEmptyBody() {

		// Call
		final var response = webTestClient.post().uri(builder -> builder.path(PATH_PREFIX + PATH_SMS).build(Map.of("id", ERRAND_ID)))
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
			tuple("recipient", "must match the regular expression ^07[02369]\\d{7}$"),
			tuple("sender", "must not be null"));

		// Verification
		verifyNoInteractions(serviceMock);
	}

	@Test
	void sendSmsWithInvalidValues() {

		// Call
		final var response = webTestClient.post().uri(builder -> builder.path(PATH_PREFIX + PATH_SMS).build(Map.of("id", ERRAND_ID)))
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
			tuple("recipient", "must match the regular expression ^07[02369]\\d{7}$"),
			tuple("sender", "size must be between 1 and 11"));

		// Verification
		verifyNoInteractions(serviceMock);
	}

	@Test
	void sendEmailWithoutBody() {

		// Call
		final var response = webTestClient.post().uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL).build(Map.of("id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("""
			Required request body is missing: public org.springframework.http.ResponseEntity<java.lang.Void>\s\
			se.sundsvall.supportmanagement.api.ErrandCommunicationResource.sendEmail(java.lang.String,\
			se.sundsvall.supportmanagement.api.model.communication.EmailRequest)""");

		// Verification
		verifyNoInteractions(serviceMock);
	}

	@Test
	void sendEmailWithEmptyBody() {

		// Call
		final var response = webTestClient.post().uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL).build(Map.of("id", ERRAND_ID)))
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
		final var response = webTestClient.post().uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL).build(Map.of("id", ERRAND_ID)))
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
		final var response = webTestClient.post().uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL).build(Map.of("id", ERRAND_ID)))
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
		final var response = webTestClient.post().uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL).build(Map.of("id", ERRAND_ID)))
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

	private static SmsRequest smsRequest() {
		return SmsRequest.create()
			.withMessage("message")
			.withRecipient("0701234567")
			.withSender("sender");
	}

	private static EmailRequest emailRequest() {
		return EmailRequest.create()
			.withMessage("message")
			.withRecipient("recipient@recipient.com")
			.withSender("sender@sender.com")
			.withSubject("subject");
	}
}

package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.messaging.EmailAttachment;
import se.sundsvall.supportmanagement.api.model.messaging.EmailRequest;
import se.sundsvall.supportmanagement.api.model.messaging.SmsRequest;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MessagingResourceFailureTest {

	private static final String CONSTRAINT_VIOLATION = "Constraint Violation";
	private static final String PATH = "/messaging/";

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void sendSmsWithoutBody() {

		// Call
		final var response = webTestClient.post().uri(PATH + "sms")
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
			se.sundsvall.supportmanagement.api.MessagingResource.sendSms(se.sundsvall.supportmanagement.api.model.messaging.SmsRequest)""");

		// Verification
		// TODO: Add verification when service layer is in place
	}

	@Test
	void sendSmsWithEmptyBody() {

		// Call
		final var response = webTestClient.post().uri(PATH + "sms")
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
		// TODO: Add verification when service layer is in place
	}

	@Test
	void sendSmsWithInvalidValues() {

		// Call
		final var response = webTestClient.post().uri(PATH + "sms")
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
		// TODO: Add verification when service layer is in place
	}

	@Test
	void sendEmailWithoutBody() {

		// Call
		final var response = webTestClient.post().uri(PATH + "email")
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
			se.sundsvall.supportmanagement.api.MessagingResource.sendEmail(se.sundsvall.supportmanagement.api.model.messaging.EmailRequest)""");

		// Verification
		// TODO: Add verification when service layer is in place
	}

	@Test
	void sendEmailWithEmptyBody() {

		// Call
		final var response = webTestClient.post().uri(PATH + "email")
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
		// TODO: Add verification when service layer is in place
	}

	@Test
	void sendEmailWithInvalidValues() {

		// Call
		final var response = webTestClient.post().uri(PATH + "email")
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
		// TODO: Add verification when service layer is in place
	}

	@Test
	void sendEmailWithEmptyAttachment() {

		// Call
		final var response = webTestClient.post().uri(PATH + "email")
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
		// TODO: Add verification when service layer is in place
	}

	@Test
	void sendEmailWithInvalidAttachmentString() {

		// Call
		final var response = webTestClient.post().uri(PATH + "email")
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
		// TODO: Add verification when service layer is in place
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

package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.communication.EmailAttachment;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandCommunicationResourceTest {

	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH_PREFIX = "/errands/{id}/communication";
	private static final String PATH_SMS = "/sms";
	private static final String PATH_EMAIL = "/email";

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void sendSms() {

		// Parameter values
		final var requestBody = smsRequest();

		// Call
		webTestClient.post().uri(builder -> builder.path(PATH_PREFIX + PATH_SMS).build(Map.of("id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		// Verification
		// TODO: Add verification when service layer is in place
	}

	@Test
	void sendEmail() {

		// Parameter values
		final var requestBody = emailRequest(false);

		webTestClient.post().uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL).build(Map.of("id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		// Verification
		// TODO: Add verification when service layer is in place
	}

	@Test
	void sendEmailWithAttachment() {

		// Parameter values
		final var requestBody = emailRequest(true);

		webTestClient.post().uri(builder -> builder.path(PATH_PREFIX + PATH_EMAIL).build(Map.of("id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		// Verification
		// TODO: Add verification when service layer is in place
	}

	private static SmsRequest smsRequest() {
		return SmsRequest.create()
			.withMessage("message")
			.withRecipient("0701234567")
			.withSender("sender");
	}

	private static EmailRequest emailRequest(boolean withAttachment) {
		return EmailRequest.create()
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

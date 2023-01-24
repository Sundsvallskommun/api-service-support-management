package se.sundsvall.supportmanagement.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentHeader;
import se.sundsvall.supportmanagement.service.ErrandAttachmentService;

import java.util.Map;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandAttachmentsResourceFailureTest {

	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/errands/{id}/attachments/";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private ErrandAttachmentService errandAttachmentServiceMock;

	@Test
	void readErrandAttachmentWithInvalidId() {

		// Parameters
		final var id = "invalid-uuid";
		final var attachmentId = randomUUID().toString();

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH.concat("{attachmentId}")).build(Map.of("id", id, "attachmentId", attachmentId)))
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
			.containsExactlyInAnyOrder(tuple("readErrandAttachment.id", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void readErrandAttachmentWithInvalidAttachmentId() {

		// Parameters
		final var attachmentId = "invalid-uuid";

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH.concat("{attachmentId}")).build(Map.of("id", ERRAND_ID, "attachmentId", attachmentId)))
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
			.containsExactlyInAnyOrder(tuple("readErrandAttachment.attachmentId", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void readErrandAttachmentsWithInvalidId() {

		// Parameters
		final var id = "invalid-uuid";

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("id", id)))
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
			.containsExactlyInAnyOrder(tuple("readErrandAttachments.id", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void createErrandAttachmentInvalidId() {

		// Parameters
		final var id = "invalid-uuid";
		final var requestBody = ErrandAttachment.create()
			.withErrandAttachmentHeader(ErrandAttachmentHeader.create().withId("id").withFileName("test.txt"))
			.withMimeType("mimeType")
			.withBase64EncodedString("file");

		// Call
		final var response = webTestClient.post().uri(builder -> builder.path(PATH).build(Map.of("id", id)))
			.contentType(APPLICATION_JSON)
			.bodyValue(requestBody)
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
			.containsExactlyInAnyOrder(tuple("createErrandAttachment.id", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void createErrandAttachmentInvalidRequestBody() {

		// Parameters
		final var requestBody = ErrandAttachment.create();

		// Call
		final var response = webTestClient.post().uri(builder -> builder.path(PATH).build(Map.of("id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(requestBody)
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
			.containsExactlyInAnyOrder(
				tuple("base64EncodedString", "must not be null"),
				tuple("base64EncodedString", "not a valid BASE64-encoded string"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void deleteErrandAttachmentWithInvalidId() {

		// Parameters
		final var id = "invalid-uuid";
		final var attachmentId = randomUUID().toString();

		// Call
		final var response = webTestClient.delete().uri(builder -> builder.path(PATH.concat("{attachmentId}")).build(Map.of("id", id, "attachmentId", attachmentId)))
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
			.containsExactlyInAnyOrder(tuple("deleteErrandAttachment.id", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void deleteErrandAttachmenteWithInvalidAttachmentId() {

		// Parameters
		final var attachmentId = "invalid-uuid";

		// Call
		final var response = webTestClient.delete().uri(builder -> builder.path(PATH.concat("{attachmentId}")).build(Map.of("id", ERRAND_ID, "attachmentId", attachmentId)))
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
			.containsExactlyInAnyOrder(tuple("deleteErrandAttachment.attachmentId", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}
}

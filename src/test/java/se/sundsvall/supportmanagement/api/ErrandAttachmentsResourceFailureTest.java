package se.sundsvall.supportmanagement.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.ErrandAttachmentService;

import java.util.Map;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.zalando.problem.Status.BAD_REQUEST;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandAttachmentsResourceFailureTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String ATTACHMENT_ID = randomUUID().toString();
	private static final String INVALID = "#invalid#";

	private static final String PATH = "/{namespace}/{municipalityId}/errands/{id}/attachments";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private ErrandAttachmentService errandAttachmentServiceMock;

	@Test
	void readErrandAttachmentWithInvalidNamespace() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
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
			.containsExactlyInAnyOrder(tuple("readErrandAttachment.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void readErrandAttachmentWithInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
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
			.containsExactlyInAnyOrder(tuple("readErrandAttachment.municipalityId", "not a valid municipality ID"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void readErrandAttachmentWithInvalidId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID, "attachmentId", ATTACHMENT_ID)))
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

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "attachmentId", INVALID)))
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
	void readErrandAttachmentsWithInvalidNamespace() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
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
			.containsExactlyInAnyOrder(tuple("readErrandAttachments.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void readErrandAttachmentsWithInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", ERRAND_ID)))
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
			.containsExactlyInAnyOrder(tuple("readErrandAttachments.municipalityId", "not a valid municipality ID"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void readErrandAttachmentsWithInvalidId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID)))
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
	void createErrandAttachmentInvalidNamespace() {

		// Parameters
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("errandAttachment", "file").filename("test.txt").contentType(TEXT_PLAIN);
		final var body = multipartBodyBuilder.build();

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(MULTIPART_FORM_DATA)
			.body(BodyInserters.fromMultipartData(body))
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
			.containsExactlyInAnyOrder(tuple("createErrandAttachment.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void createErrandAttachmentInvalidMunicipalityId() {

		// Parameters
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("errandAttachment", "file").filename("test.txt").contentType(TEXT_PLAIN);
		final var body = multipartBodyBuilder.build();

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", ERRAND_ID)))
			.contentType(MULTIPART_FORM_DATA)
			.body(BodyInserters.fromMultipartData(body))
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
			.containsExactlyInAnyOrder(tuple("createErrandAttachment.municipalityId", "not a valid municipality ID"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void createErrandAttachmentInvalidId() {

		// Parameters
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("errandAttachment", "file").filename("test.txt").contentType(TEXT_PLAIN);
		final var body = multipartBodyBuilder.build();

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID)))
			.contentType(MULTIPART_FORM_DATA)
			.body(BodyInserters.fromMultipartData(body))
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
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("noSuchPart", "invalid");
		final var body = multipartBodyBuilder.build();

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(MULTIPART_FORM_DATA)
			.body(BodyInserters.fromMultipartData(body))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Bad Request");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("Required part 'errandAttachment' is not present.");

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void deleteErrandAttachmentWithInvalidNamespace() {

		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
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
			.containsExactlyInAnyOrder(tuple("deleteErrandAttachment.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void deleteErrandAttachmentWithInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
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
			.containsExactlyInAnyOrder(tuple("deleteErrandAttachment.municipalityId", "not a valid municipality ID"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void deleteErrandAttachmentWithInvalidId() {

		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID, "attachmentId", ATTACHMENT_ID)))
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

		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID, "attachmentId", INVALID)))
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

package se.sundsvall.supportmanagement.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.ErrandAttachmentService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.TOO_MANY_REQUESTS;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandAttachmentsResourceFailureTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String ATTACHMENT_ID = randomUUID().toString();
	private static final String INVALID = "#invalid#";

	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/attachments";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandAttachmentService errandAttachmentServiceMock;

	@Test
	void readErrandAttachmentWithInvalidNamespace() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
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
			.containsExactlyInAnyOrder(tuple("readErrandAttachment.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void readErrandAttachmentWithInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
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
	void readErrandAttachmentWithInvalidErrandId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", INVALID, "attachmentId", ATTACHMENT_ID)))
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
			.containsExactlyInAnyOrder(tuple("readErrandAttachment.errandId", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void readErrandAttachmentWithInvalidAttachmentId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "attachmentId", INVALID)))
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
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
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
			.containsExactlyInAnyOrder(tuple("readErrandAttachments.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void readErrandAttachmentsWithInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID)))
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
	void readErrandAttachmentsWithInvalidErrandId() {

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", INVALID)))
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
			.containsExactlyInAnyOrder(tuple("readErrandAttachments.errandId", "not a valid UUID"));

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
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
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
			.containsExactlyInAnyOrder(tuple("createErrandAttachment.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

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
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID)))
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
	void createErrandAttachmentInvalidErrandId() {

		// Parameters
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("errandAttachment", "file").filename("test.txt").contentType(TEXT_PLAIN);
		final var body = multipartBodyBuilder.build();

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", INVALID)))
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
			.containsExactlyInAnyOrder(tuple("createErrandAttachment.errandId", "not a valid UUID"));

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
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
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
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
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
			.containsExactlyInAnyOrder(tuple("deleteErrandAttachment.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void deleteErrandAttachmentWithInvalidMunicipalityId() {

		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
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
	void deleteErrandAttachmentWithInvalidErrandId() {

		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", INVALID, "attachmentId", ATTACHMENT_ID)))
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
			.containsExactlyInAnyOrder(tuple("deleteErrandAttachment.errandId", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandAttachmentServiceMock);
	}

	@Test
	void deleteErrandAttachmentsWithInvalidAttachmentId() {

		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("/{attachmentId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "attachmentId", INVALID)))
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

	@Test
	void readErrandAttachmentServiceBusy() {

		// Parameter values
		final var attachmentId = randomUUID().toString();
		doThrow(Problem.valueOf(TOO_MANY_REQUESTS, "Service is currently unavailable, please try again later."))
			.when(errandAttachmentServiceMock).readErrandAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(attachmentId), any());

		// ACT
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH.concat("/{attachmentId}"))
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", attachmentId)))
			.exchange()
			.expectStatus().is4xxClientError()
			.expectBody(Problem.class)
			.returnResult().getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Too Many Requests");
		assertThat(response.getStatus()).isEqualTo(TOO_MANY_REQUESTS);
		assertThat(response.getDetail()).isEqualTo("Service is currently unavailable, please try again later.");

		// Verification
		verify(errandAttachmentServiceMock).readErrandAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(attachmentId), any());
	}

}

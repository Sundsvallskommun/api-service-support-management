package se.sundsvall.supportmanagement.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentHeader;
import se.sundsvall.supportmanagement.service.ErrandAttachmentService;

import java.util.List;
import java.util.Map;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandAttachmentsResourceTest {

	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/errands/{id}/attachments/";

	@MockBean
	private ErrandAttachmentService errandAttachmentServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@LocalServerPort
	private int port;

	@Test
	void createErrandAttachment() {

		// Parameter values
		final var fileName = "test.txt";
		final var file = "test";
		final var mimeType = "text/plain";
		final var attachmentId = "attachmentId";
		final var requestBody = ErrandAttachment.create()
			.withErrandAttachmentHeader(ErrandAttachmentHeader.create()
				.withFileName(fileName)
				.withMimeType(mimeType))
			.withBase64EncodedString(file);

		// Mock
		when(errandAttachmentServiceMock.createErrandAttachment(ERRAND_ID, requestBody)).thenReturn(attachmentId);

		// Call
		webTestClient.post().uri(builder -> builder.path(PATH).build(Map.of("id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("http://localhost:".concat(String.valueOf(port)).concat(fromPath("/errands/{id}/attachments/{attachmentId}").build(Map.of("id", ERRAND_ID, "attachmentId", attachmentId)).toString()))
			.expectBody().isEmpty();

		// Verification
		verify(errandAttachmentServiceMock).createErrandAttachment(ERRAND_ID, requestBody);
	}

	@Test
	void readErrandAttachment() {

		// Parameter values
		final var attachmentId = randomUUID().toString();
		final var errandAttachment = ErrandAttachment.create()
			.withErrandAttachmentHeader(ErrandAttachmentHeader.create()
				.withFileName("test.txt")
				.withId(attachmentId)
				.withMimeType("text/plain"))
			.withBase64EncodedString("test");

		// Mock
		when(errandAttachmentServiceMock.readErrandAttachment(ERRAND_ID, attachmentId)).thenReturn(errandAttachment);

		final var response = webTestClient.get().uri(builder -> builder.path(PATH.concat("{attachmentId}")).build(Map.of("id", ERRAND_ID, "attachmentId", attachmentId)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(ErrandAttachment.class)
			.returnResult();

		assertThat(response.getResponseBody()).isEqualTo(errandAttachment);

		// Verification
		verify(errandAttachmentServiceMock).readErrandAttachment(ERRAND_ID, attachmentId);
	}

	@Test
	void readErrandAttachments() {
		//Parameter values
		final var errandAttachments = List.of(ErrandAttachmentHeader.create()
				.withFileName("test.txt")
				.withId(randomUUID().toString())
				.withMimeType("text/plain"));

		when(errandAttachmentServiceMock.readErrandAttachmentHeaders(ERRAND_ID)).thenReturn(errandAttachments);

		final var response = webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("id", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(ErrandAttachmentHeader.class)
			.returnResult();

		assertThat(response.getResponseBody()).isEqualTo(errandAttachments);

		// Verification
		verify(errandAttachmentServiceMock).readErrandAttachmentHeaders(ERRAND_ID);
	}

	@Test
	void deleteErrandAttachment() {

		// Parameter values
		final var attachmentId = randomUUID().toString();

		webTestClient.delete().uri(builder -> builder.path(PATH.concat("{attachmentId}")).build(Map.of("id", ERRAND_ID, "attachmentId", attachmentId)))
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().doesNotExist(CONTENT_TYPE);

		// Verification
		verify(errandAttachmentServiceMock).deleteErrandAttachment(ERRAND_ID, attachmentId);
	}

}

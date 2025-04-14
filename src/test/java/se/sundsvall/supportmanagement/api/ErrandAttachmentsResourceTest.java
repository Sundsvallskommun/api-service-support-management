package se.sundsvall.supportmanagement.api;

import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.service.ErrandAttachmentService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandAttachmentsResourceTest {

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String ERRAND_ID = randomUUID().toString();

	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/attachments";

	@MockitoBean
	private ErrandAttachmentService errandAttachmentServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createErrandAttachment() {

		// Parameter values
		final var fileName = "test.txt";
		final var file = "test";
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("errandAttachment", file).filename(fileName).contentType(TEXT_PLAIN);
		final var body = multipartBodyBuilder.build();
		final var attachmentId = "attachmentId";

		// Mock
		when(errandAttachmentServiceMock.createErrandAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), any())).thenReturn(attachmentId);

		// Call
		webTestClient.post().uri(builder -> builder.path(PATH)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(MULTIPART_FORM_DATA)
			.accept(APPLICATION_JSON)
			.body(BodyInserters.fromMultipartData(body))
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/attachments/" + attachmentId)
			.expectBody().isEmpty();

		// Verification
		final ArgumentCaptor<MultipartFile> fileArgumentCaptor = ArgumentCaptor.forClass(MultipartFile.class);
		verify(errandAttachmentServiceMock).createErrandAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), fileArgumentCaptor.capture());
		final var multipartFile = fileArgumentCaptor.getValue();
		assertThat(multipartFile.getOriginalFilename()).isEqualTo(fileName);
		assertThat(multipartFile.getContentType()).isEqualTo(TEXT_PLAIN_VALUE);
	}

	@Test
	void readErrandAttachment() {

		// Parameter values
		final var attachmentId = randomUUID().toString();

		webTestClient.get().uri(builder -> builder.path(PATH.concat("/{attachmentId}"))
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", attachmentId)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk();

		// Verification
		verify(errandAttachmentServiceMock).readErrandAttachment(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(attachmentId), any());
	}

	@Test
	void readErrandAttachments() {
		// Parameter values
		final var errandAttachments = List.of(ErrandAttachment.create()
			.withFileName("test.txt")
			.withCreated(now())
			.withId(randomUUID().toString())
			.withMimeType("text/plain"));

		when(errandAttachmentServiceMock.readErrandAttachments(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(errandAttachments);

		final var response = webTestClient.get().uri(builder -> builder.path(PATH)
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(ErrandAttachment.class)
			.returnResult();

		assertThat(response.getResponseBody()).isEqualTo(errandAttachments);

		// Verification
		verify(errandAttachmentServiceMock).readErrandAttachments(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
	}

	@Test
	void deleteErrandAttachment() {

		// Parameter values
		final var attachmentId = randomUUID().toString();

		webTestClient.delete().uri(builder -> builder.path(PATH.concat("/{attachmentId}"))
			.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", attachmentId)))
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().contentType(ALL_VALUE);

		// Verification
		verify(errandAttachmentServiceMock).deleteErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, attachmentId);
	}
}

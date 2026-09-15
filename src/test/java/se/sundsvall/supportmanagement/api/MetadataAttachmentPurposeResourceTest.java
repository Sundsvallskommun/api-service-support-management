package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.AttachmentPurpose;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataAttachmentPurposeResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/attachmentpurposes";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ID = "5f79a808-0ef3-4985-99b9-b12f23e202a7";

	private static final Map<String, String> PATH_VARIABLES = Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ID);

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createAttachmentPurpose() {

		// Arrange
		final var purpose = AttachmentPurpose.create().withName("SUPPORTING").withDisplayName("Underlag");
		when(metadataServiceMock.createAttachmentPurpose(NAMESPACE, MUNICIPALITY_ID, purpose)).thenReturn(ID);

		// Act & Verify
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(purpose)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/attachmentpurposes/" + ID)
			.expectBody().isEmpty();

		verify(metadataServiceMock).createAttachmentPurpose(NAMESPACE, MUNICIPALITY_ID, purpose);
	}

	@Test
	void getAttachmentPurpose() {

		// Arrange
		final var purpose = AttachmentPurpose.create().withId(ID).withName("SUPPORTING");
		when(metadataServiceMock.getAttachmentPurpose(NAMESPACE, MUNICIPALITY_ID, ID)).thenReturn(purpose);

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/{id}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(AttachmentPurpose.class)
			.returnResult()
			.getResponseBody();

		// Verify
		assertThat(response).isEqualTo(purpose);
		verify(metadataServiceMock).getAttachmentPurpose(NAMESPACE, MUNICIPALITY_ID, ID);
	}

	@Test
	void getAttachmentPurposes() {

		// Arrange
		when(metadataServiceMock.findAttachmentPurposes(any(), any(), any(Sort.class)))
			.thenReturn(List.of(AttachmentPurpose.create().withId(ID).withName("SUPPORTING")));

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(AttachmentPurpose.class)
			.returnResult()
			.getResponseBody();

		// Verify
		assertThat(response).hasSize(1);
		verify(metadataServiceMock).findAttachmentPurposes(any(), any(), any(Sort.class));
	}

	@Test
	void updateAttachmentPurpose() {

		// Arrange
		final var purpose = AttachmentPurpose.create().withName("SUPPORTING").withDeprecated(true);
		when(metadataServiceMock.updateAttachmentPurpose(NAMESPACE, MUNICIPALITY_ID, ID, purpose)).thenReturn(purpose.withId(ID));

		// Act & Verify
		webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(purpose)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(AttachmentPurpose.class);

		verify(metadataServiceMock).updateAttachmentPurpose(NAMESPACE, MUNICIPALITY_ID, ID, purpose);
	}

	@Test
	void deleteAttachmentPurpose() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{id}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().contentType(ALL)
			.expectBody().isEmpty();

		verify(metadataServiceMock).deleteAttachmentPurpose(NAMESPACE, MUNICIPALITY_ID, ID);
	}
}

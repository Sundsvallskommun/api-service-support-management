package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.Labels;
import se.sundsvall.supportmanagement.service.MetadataService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataLabelResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/labels";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createLabels() {
		// Arrange
		final var labels = List.of(
			Label.create().withClassification("classification").withName("name_1"),
			Label.create().withClassification("classification").withName("name_2"));

		// Act
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(labels)
			.exchange()
			.expectStatus().isAccepted()
			.expectBody().isEmpty();

		// Assert and verify
		verify(metadataServiceMock).createLabels(NAMESPACE, MUNICIPALITY_ID, labels);
		verifyNoMoreInteractions(metadataServiceMock);
	}

	@Test
	void getLabels() {
		// Arrange
		final var labels = Labels.create().withLabelStructure(List.of(Label.create().withClassification("classification").withName("name")));
		when(metadataServiceMock.findLabels(NAMESPACE, MUNICIPALITY_ID)).thenReturn(labels);

		// Act
		final var response = webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Labels.class)
			.returnResult();

		// Assert and verify
		assertThat(response.getResponseBody()).isEqualTo(labels);
		verify(metadataServiceMock).findLabels(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(metadataServiceMock);
	}

	@Test
	void UpdateLabels() {
		// Arrange
		final var labels = List.of(
			Label.create().withClassification("classification").withName("name_1"),
			Label.create().withClassification("classification").withName("name_2"));

		// Act
		webTestClient.put()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(labels)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		// Assert and verify
		verify(metadataServiceMock).updateLabels(NAMESPACE, MUNICIPALITY_ID, labels);
		verifyNoMoreInteractions(metadataServiceMock);
	}

	@Test
	void deleteLabels() {
		// Act
		webTestClient.delete().uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

		// Assert and verify
		verify(metadataServiceMock).deleteLabels(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(metadataServiceMock);
	}
}

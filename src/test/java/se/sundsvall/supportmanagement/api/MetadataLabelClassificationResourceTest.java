package se.sundsvall.supportmanagement.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.api.model.metadata.LabelClassification;
import se.sundsvall.supportmanagement.service.LabelClassificationService;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ResourceTest
class MetadataLabelClassificationResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/label-classifications";

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String CLASSIFICATION = "subtype";

	@Autowired
	private LabelClassificationService labelClassificationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createLabelClassification() {
		// Setup
		final var body = LabelClassification.create().withClassification(CLASSIFICATION).withDisplayName("Undertyp");

		// Mock
		when(labelClassificationServiceMock.createLabelClassification(NAMESPACE, MUNICIPALITY_ID, body)).thenReturn(CLASSIFICATION);

		// Call
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/label-classifications/" + CLASSIFICATION)
			.expectBody().isEmpty();

		// Verifications & assertions
		verify(labelClassificationServiceMock).createLabelClassification(NAMESPACE, MUNICIPALITY_ID, body);
	}

	@Test
	void getLabelClassification() {
		// Setup
		final var labelClassification = LabelClassification.create().withId("5f79a808-0ef3-4985-99b9-b12f23e202a7").withClassification(CLASSIFICATION).withDisplayName("Undertyp");

		// Mock
		when(labelClassificationServiceMock.getLabelClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION)).thenReturn(labelClassification);

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/{classification}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "classification", CLASSIFICATION)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(LabelClassification.class)
			.returnResult()
			.getResponseBody();

		// Verifications & assertions
		verify(labelClassificationServiceMock).getLabelClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION);
		assertThat(response).isNotNull().isEqualTo(labelClassification);
	}

	@Test
	void getLabelClassifications() {
		// Call
		webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String[].class)
			.isEqualTo(EMPTY_STRING_ARRAY);

		// Verifications & assertions
		verify(labelClassificationServiceMock).findLabelClassifications(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(Sort.class));
	}

	@Test
	void updateLabelClassification() {
		// Setup
		final var body = LabelClassification.create().withDisplayName("Undertyp");
		final var updated = LabelClassification.create().withClassification(CLASSIFICATION).withDisplayName("Undertyp");

		// Mock
		when(labelClassificationServiceMock.updateLabelClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION, body)).thenReturn(updated);

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{classification}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "classification", CLASSIFICATION)))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.OK)
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(LabelClassification.class)
			.returnResult()
			.getResponseBody();

		// Verifications & assertions
		verify(labelClassificationServiceMock).updateLabelClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION, body);
		assertThat(response).isNotNull().isEqualTo(updated);
	}

	@Test
	void deleteLabelClassification() {
		// Call
		webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{classification}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "classification", CLASSIFICATION)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

		// Verifications & assertions
		verify(labelClassificationServiceMock).deleteLabelClassification(NAMESPACE, MUNICIPALITY_ID, CLASSIFICATION);
	}
}

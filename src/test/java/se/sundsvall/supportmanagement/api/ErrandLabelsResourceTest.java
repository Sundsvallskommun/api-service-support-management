package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.ErrandLabelService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandLabelsResourceTest {

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String ERRAND_ID = randomUUID().toString();

	private static final String PATH = "/{municipalityId}/{namespace}/errands/{id}/labels";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private ErrandLabelService errandLabelServiceMock;

	@Test
	void createErrandLabels() {
		// Parameter values
		final var labels = List.of("label1", "label2");

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.accept(ALL)
			.bodyValue(labels)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/labels")
			.expectBody().isEmpty();

		// Verification
		assertThat(response).isNotNull();
		verify(errandLabelServiceMock).createErrandLabels(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, labels);
	}

	@Test
	void getErrandLabels() {
		// Mock
		when(errandLabelServiceMock.getErrandLabels(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(List.of("label1", "label2"));

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(String.class)
			.returnResult();

		// Verification
		assertThat(response).isNotNull();
		assertThat(response.getResponseBody()).containsExactly("[ \"label1\", \"label2\" ]");
		verify(errandLabelServiceMock).getErrandLabels(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
	}

	@Test
	void updateErrandLabels() {
		// Parameter values
		final var labels = List.of("label1", "label2");

		// Call
		webTestClient.put()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.accept(ALL)
			.bodyValue(labels)
			.exchange()
			.expectStatus().isNoContent();

		// Verification
		verify(errandLabelServiceMock).updateErrandLabel(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, labels);
	}

	@Test
	void deleteErrandLabels() {
		// Call
		webTestClient.delete()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ERRAND_ID)))
			.accept(ALL)
			.exchange()
			.expectStatus().isNoContent();

		// Verification
		verify(errandLabelServiceMock).deleteErrandLabel(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
	}

}

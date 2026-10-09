package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.api.model.metadata.DecisionOutcome;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ResourceTest
class MetadataDecisionOutcomeResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/decisionoutcomes";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ID = "5f79a808-0ef3-4985-99b9-b12f23e202a7";

	private static final Map<String, String> PATH_VARIABLES = Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ID);

	@Autowired
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createDecisionOutcome() {

		// Arrange
		final var outcome = DecisionOutcome.create().withName("APPROVAL").withDisplayName("Bifall");
		when(metadataServiceMock.createDecisionOutcome(NAMESPACE, MUNICIPALITY_ID, outcome)).thenReturn(ID);

		// Act & Verify
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(outcome)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/decisionoutcomes/" + ID)
			.expectBody().isEmpty();

		verify(metadataServiceMock).createDecisionOutcome(NAMESPACE, MUNICIPALITY_ID, outcome);
	}

	@Test
	void createDecisionOutcomeWithoutName() {

		// Act & Verify
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(DecisionOutcome.create().withDisplayName("Bifall"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void getDecisionOutcome() {

		// Arrange
		final var outcome = DecisionOutcome.create().withId(ID).withName("APPROVAL");
		when(metadataServiceMock.getDecisionOutcome(NAMESPACE, MUNICIPALITY_ID, ID)).thenReturn(outcome);

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/{id}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(DecisionOutcome.class)
			.returnResult()
			.getResponseBody();

		// Verify
		assertThat(response).isEqualTo(outcome);
		verify(metadataServiceMock).getDecisionOutcome(NAMESPACE, MUNICIPALITY_ID, ID);
	}

	@Test
	void getDecisionOutcomes() {

		// Arrange
		when(metadataServiceMock.findDecisionOutcomes(any(), any(), any(Sort.class)))
			.thenReturn(List.of(DecisionOutcome.create().withId(ID).withName("APPROVAL")));

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(DecisionOutcome.class)
			.returnResult()
			.getResponseBody();

		// Verify
		assertThat(response).hasSize(1);
		verify(metadataServiceMock).findDecisionOutcomes(any(), any(), any(Sort.class));
	}

	@Test
	void updateDecisionOutcome() {

		// Arrange
		final var outcome = DecisionOutcome.create().withName("APPROVAL").withDeprecated(true);
		when(metadataServiceMock.updateDecisionOutcome(NAMESPACE, MUNICIPALITY_ID, ID, outcome)).thenReturn(outcome.withId(ID));

		// Act & Verify
		webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(outcome)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(DecisionOutcome.class);

		verify(metadataServiceMock).updateDecisionOutcome(NAMESPACE, MUNICIPALITY_ID, ID, outcome);
	}

	@Test
	void deleteDecisionOutcome() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{id}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().contentType(ALL)
			.expectBody().isEmpty();

		verify(metadataServiceMock).deleteDecisionOutcome(NAMESPACE, MUNICIPALITY_ID, ID);
	}
}

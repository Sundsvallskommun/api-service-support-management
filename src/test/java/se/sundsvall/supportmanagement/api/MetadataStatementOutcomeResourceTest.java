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
import se.sundsvall.supportmanagement.api.model.metadata.StatementOutcome;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataStatementOutcomeResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/statementoutcomes";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ID = "5f79a808-0ef3-4985-99b9-b12f23e202a7";

	private static final Map<String, String> PATH_VARIABLES = Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ID);

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createStatementOutcome() {

		// Arrange
		final var outcome = StatementOutcome.create().withName("NO_RESPONSE").withDisplayName("Inget svar").withResponded(false);
		when(metadataServiceMock.createStatementOutcome(NAMESPACE, MUNICIPALITY_ID, outcome)).thenReturn(ID);

		// Act & Verify
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(outcome)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/statementoutcomes/" + ID)
			.expectBody().isEmpty();

		verify(metadataServiceMock).createStatementOutcome(NAMESPACE, MUNICIPALITY_ID, outcome);
	}

	@Test
	void createStatementOutcomeWithoutName() {

		// Act & Verify
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(StatementOutcome.create().withDisplayName("Tillstyrker"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void getStatementOutcome() {

		// Arrange
		final var outcome = StatementOutcome.create().withId(ID).withName("SUPPORTS").withResponded(true);
		when(metadataServiceMock.getStatementOutcome(NAMESPACE, MUNICIPALITY_ID, ID)).thenReturn(outcome);

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/{id}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(StatementOutcome.class)
			.returnResult()
			.getResponseBody();

		// Verify
		assertThat(response).isEqualTo(outcome);
		verify(metadataServiceMock).getStatementOutcome(NAMESPACE, MUNICIPALITY_ID, ID);
	}

	@Test
	void getStatementOutcomes() {

		// Arrange
		when(metadataServiceMock.findStatementOutcomes(any(), any(), any(Sort.class)))
			.thenReturn(List.of(StatementOutcome.create().withId(ID).withName("SUPPORTS")));

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(StatementOutcome.class)
			.returnResult()
			.getResponseBody();

		// Verify
		assertThat(response).hasSize(1);
		verify(metadataServiceMock).findStatementOutcomes(any(), any(), any(Sort.class));
	}

	@Test
	void updateStatementOutcome() {

		// Arrange
		final var outcome = StatementOutcome.create().withName("SUPPORTS").withDeprecated(true);
		when(metadataServiceMock.updateStatementOutcome(NAMESPACE, MUNICIPALITY_ID, ID, outcome)).thenReturn(outcome.withId(ID));

		// Act & Verify
		webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(outcome)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(StatementOutcome.class);

		verify(metadataServiceMock).updateStatementOutcome(NAMESPACE, MUNICIPALITY_ID, ID, outcome);
	}

	@Test
	void deleteStatementOutcome() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{id}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().contentType(ALL)
			.expectBody().isEmpty();

		verify(metadataServiceMock).deleteStatementOutcome(NAMESPACE, MUNICIPALITY_ID, ID);
	}
}

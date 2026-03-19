package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.PhaseTransition;
import se.sundsvall.supportmanagement.service.MetadataService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataPhaseTransitionResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/phases/{phaseId}/transitions";

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String PHASE_ID = randomUUID().toString();

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createTransition() {
		final var transitionId = randomUUID().toString();
		final var body = PhaseTransition.create()
			.withTargetPhaseId(randomUUID().toString())
			.withDescription("Skicka till utredning");

		when(metadataServiceMock.createPhaseTransition(NAMESPACE, MUNICIPALITY_ID, PHASE_ID, body)).thenReturn(transitionId);

		webTestClient.post().uri(builder -> builder.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "phaseId", PHASE_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/phases/" + PHASE_ID + "/transitions/" + transitionId)
			.expectBody().isEmpty();
	}

	@Test
	void getTransitions() {
		final var transition1 = PhaseTransition.create().withId(randomUUID().toString()).withTargetPhaseId(randomUUID().toString());
		final var transition2 = PhaseTransition.create().withId(randomUUID().toString()).withTargetPhaseId(randomUUID().toString());
		final var transitions = List.of(transition1, transition2);

		when(metadataServiceMock.findPhaseTransitions(NAMESPACE, MUNICIPALITY_ID, PHASE_ID)).thenReturn(transitions);

		final var result = webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "phaseId", PHASE_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(PhaseTransition.class)
			.returnResult()
			.getResponseBody();

		assertThat(result).hasSize(2);
	}

	@Test
	void deleteTransition() {
		final var transitionId = randomUUID().toString();

		webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{transitionId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "phaseId", PHASE_ID, "transitionId", transitionId)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

		verify(metadataServiceMock).deletePhaseTransition(NAMESPACE, MUNICIPALITY_ID, PHASE_ID, transitionId);
	}
}

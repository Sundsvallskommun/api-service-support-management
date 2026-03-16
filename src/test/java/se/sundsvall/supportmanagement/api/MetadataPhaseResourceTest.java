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
import se.sundsvall.supportmanagement.api.model.metadata.Phase;
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
class MetadataPhaseResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/phases";

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createPhase() {
		final var phaseId = randomUUID().toString();
		final var body = Phase.create()
			.withName("INVESTIGATION")
			.withDisplayName("Utredning")
			.withDescription("Fas för utredning")
			.withAllowedStatuses(List.of("IN_PROGRESS", "WAITING"));

		when(metadataServiceMock.createPhase(NAMESPACE, MUNICIPALITY_ID, body)).thenReturn(phaseId);

		webTestClient.post().uri(builder -> builder.path(PATH).build(MUNICIPALITY_ID, NAMESPACE))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/phases/" + phaseId)
			.expectBody().isEmpty();
	}

	@Test
	void getPhase() {
		final var phaseId = randomUUID().toString();
		final var phase = Phase.create()
			.withId(phaseId)
			.withName("INVESTIGATION");

		when(metadataServiceMock.getPhase(NAMESPACE, MUNICIPALITY_ID, phaseId)).thenReturn(phase);

		final var result = webTestClient.get().uri(builder -> builder.path(PATH + "/{phaseId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "phaseId", phaseId)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Phase.class)
			.isEqualTo(phase)
			.returnResult()
			.getResponseBody();

		assertThat(result.getName()).isNotNull().isEqualTo("INVESTIGATION");
	}

	@Test
	void getPhases() {
		final var phase1 = Phase.create().withName("INVESTIGATION");
		final var phase2 = Phase.create().withName("DECISION");
		final var phases = List.of(phase1, phase2);

		when(metadataServiceMock.findPhases(NAMESPACE, MUNICIPALITY_ID)).thenReturn(phases);

		final var result = webTestClient.get().uri(builder -> builder.path(PATH).build(MUNICIPALITY_ID, NAMESPACE))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Phase.class)
			.returnResult()
			.getResponseBody();

		assertThat(result).hasSize(2).extracting(Phase::getName).containsExactly("INVESTIGATION", "DECISION");
	}

	@Test
	void updatePhase() {
		final var phaseId = randomUUID().toString();
		final var patch = Phase.create()
			.withName("INVESTIGATION")
			.withDisplayName("Updated");

		when(metadataServiceMock.patchPhase(phaseId, NAMESPACE, MUNICIPALITY_ID, patch)).thenReturn(patch);

		webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{phaseId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "phaseId", phaseId)))
			.contentType(APPLICATION_JSON)
			.bodyValue(patch)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.OK);

		verify(metadataServiceMock).patchPhase(phaseId, NAMESPACE, MUNICIPALITY_ID, patch);
	}

	@Test
	void deletePhase() {
		final var phaseId = randomUUID().toString();

		webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{phaseId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "phaseId", phaseId)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

		verify(metadataServiceMock).deletePhase(phaseId, NAMESPACE, MUNICIPALITY_ID);
	}
}

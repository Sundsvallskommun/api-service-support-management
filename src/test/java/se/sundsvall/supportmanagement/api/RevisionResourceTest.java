package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.revision.DifferenceResponse;
import se.sundsvall.supportmanagement.api.model.revision.Revision;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class RevisionResourceTest {

	private static final String PATH = "/errands/{id}/revisions";

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getRevisions() {
		// Parameter values
		final var id = UUID.randomUUID().toString();

		final var response = webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("id", id)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Revision.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		//TODO: Add verification for call to service
	}

	@Test
	void getDifference() {
		// Parameter values
		final var id = UUID.randomUUID().toString();
		final var source = 1;
		final var target = 2;

		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/difference").queryParam("source", source).queryParam("target", target).build(Map.of("id", id)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(DifferenceResponse.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		//TODO: Add verification for call to service
	}
}

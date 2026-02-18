package se.sundsvall.supportmanagement.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataStatusResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/statuses";

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createStatus() {
		// Setup
		final var statusName = "statusName";
		final var status = Status.create().withName(statusName);

		// Mock
		when(metadataServiceMock.createStatus(NAMESPACE, MUNICIPALITY_ID, status)).thenReturn(statusName);

		// Call
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(status)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/statuses/" + statusName)
			.expectBody().isEmpty();

		// Verifications & assertions
		verify(metadataServiceMock).createStatus(NAMESPACE, MUNICIPALITY_ID, status);
	}

	@Test
	void getStatus() {
		// Setup
		final var statusName = "statusName";
		final var status = Status.create().withName(statusName);

		// Mock
		when(metadataServiceMock.getStatus(NAMESPACE, MUNICIPALITY_ID, statusName)).thenReturn(status);

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/{status}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "status", statusName)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Status.class)
			.returnResult()
			.getResponseBody();

		// Verifications & assertions
		verify(metadataServiceMock).getStatus(NAMESPACE, MUNICIPALITY_ID, statusName);
		assertThat(response).isNotNull().isEqualTo(status);
	}

	@Test
	void getStatuses() {
		// Call
		webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String[].class)
			.isEqualTo(EMPTY_STRING_ARRAY);

		// Verifications & assertions
		verify(metadataServiceMock).findStatuses(NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void deleteStatus() {
		// Setup
		final var statusName = "statusName";

		// Call
		webTestClient.delete().uri(builder -> builder.path(PATH + "/{status}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "status", statusName)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

		// Verifications & assertions
		verify(metadataServiceMock).deleteStatus(NAMESPACE, MUNICIPALITY_ID, statusName);
	}

}

package se.sundsvall.supportmanagement.api;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.service.MetadataService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataStatusResourceTest {

	@MockBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@LocalServerPort
	private int port;

	@Test
	void createStatus() {

		webTestClient.post().uri("my.namespace/2281/metadata/statuses")
			.bodyValue(Status.create().withName("name"))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED);

		// TODO: Verify when service layer is ready
	}

	@Test
	void getStatuses() {

		webTestClient.get().uri("my.namespace/2281/metadata/statuses")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String[].class)
			.isEqualTo(EMPTY_STRING_ARRAY);

		verify(metadataServiceMock).findStatuses("my.namespace", "2281");
	}

	@Test
	void deleteStatus() {

		webTestClient.delete().uri("my.namespace/2281/metadata/statuses/status-name")
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED);

		// TODO: Verify when service layer is ready
	}
}

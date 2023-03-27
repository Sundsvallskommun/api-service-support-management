package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.MetadataResponse;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.service.MetadataService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataResourceTest {

	@MockBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@LocalServerPort
	private int port;

	@Test
	void getAll() {

		// Setup
		final var metadataResponse = MetadataResponse.create()
			.withCategories(List.of(Category.create().withName("Category-1").withTypes(List.of(Type.create().withName("Type-1")))))
			.withStatuses(List.of(Status.create().withName("Status-1")))
			.withExternalIdTypes(List.of(ExternalIdType.create().withName("ExternalIdType-1")));

		when(metadataServiceMock.findAll(any(), any())).thenReturn(metadataResponse);

		// Call
		final var response = webTestClient.get().uri("my.namespace/2281/metadata")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(MetadataResponse.class)
			.returnResult().getResponseBody();

		// Verification
		assertThat(response).isEqualTo(metadataResponse);

		verify(metadataServiceMock).findAll("my.namespace", "2281");
	}
}

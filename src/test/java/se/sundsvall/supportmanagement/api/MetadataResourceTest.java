package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Map;

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
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.Labels;
import se.sundsvall.supportmanagement.api.model.metadata.MetadataResponse;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.service.MetadataService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataResourceTest {

	private static final String NAMESPACE = "my.namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PATH = "{municipalityId}/{namespace}/metadata";

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
			.withExternalIdTypes(List.of(ExternalIdType.create().withName("ExternalIdType-1")))
			.withLabels(Labels.create().withLabelStructure(List.of(Label.create().withClassification("Classification-1").withName("Name-1"))))
			.withRoles(List.of(Role.create().withName("Role-1")))
			.withStatuses(List.of(Status.create().withName("Status-1")));

		when(metadataServiceMock.findAll(any(), any())).thenReturn(metadataResponse);

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
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

package se.sundsvall.supportmanagement.api;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.service.MetadataService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataExternalIdTypeResourceTest {
	private static final String PATH = "/{namespace}/{municipalityId}/metadata/externalIdTypes";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@MockBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@LocalServerPort
	private int port;

	@Test
	void createExternalIdType() {
		// Setup
		final var externalIdTypeName = "externalIdTypeName";
		final var externalIdType = ExternalIdType.create().withName(externalIdTypeName);

		// Mock
		when(metadataServiceMock.createExternalIdType(NAMESPACE, MUNICIPALITY_ID, externalIdType)).thenReturn(externalIdTypeName);

		// Call
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(externalIdType)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("http://localhost:".concat(String.valueOf(port)).concat(fromPath(PATH + "/{externalIdType}")
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "externalIdType", externalIdTypeName)).toString()))
			.expectBody().isEmpty();

		// Verifications & assertions
		verify(metadataServiceMock).createExternalIdType(NAMESPACE, MUNICIPALITY_ID, externalIdType);
	}

	@Test
	void getExternalIdType() {
		// Setup
		final var externalIdTypeName = "externalIdTypeName";
		final var externalIdType = ExternalIdType.create().withName(externalIdTypeName);

		// Mock
		when(metadataServiceMock.getExternalIdType(NAMESPACE, MUNICIPALITY_ID, externalIdTypeName)).thenReturn(externalIdType);

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/{externalIdType}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "externalIdType", externalIdTypeName)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(ExternalIdType.class)
			.returnResult()
			.getResponseBody();

		// Verifications & assertions
		verify(metadataServiceMock).getExternalIdType(NAMESPACE, MUNICIPALITY_ID, externalIdTypeName);
		assertThat(response).isNotNull().isEqualTo(externalIdType);
	}

	@Test
	void getExternalIdTypes() {
		// Call
		webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String[].class)
			.isEqualTo(EMPTY_STRING_ARRAY);

		// Verifications & assertions
		verify(metadataServiceMock).findExternalIdTypes(NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void deleteExternalIdType() {
		// Setup
		final var externalIdTypeName = "externalIdTypeName";

		// Call
		webTestClient.delete().uri(builder -> builder.path(PATH + "/{externalIdType}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "externalIdType", externalIdTypeName)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

		// Verifications & assertions
		verify(metadataServiceMock).deleteExternalIdType(NAMESPACE, MUNICIPALITY_ID, externalIdTypeName);
	}
}

package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.api.model.metadata.ResolutionCode;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ResourceTest
class MetadataResolutionCodeResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/resolutioncodes";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ID = "5f79a808-0ef3-4985-99b9-b12f23e202a7";

	private static final Map<String, String> PATH_VARIABLES = Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", ID);

	@Autowired
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createResolutionCode() {

		// Arrange
		final var outcome = ResolutionCode.create().withName("SOLVED").withDisplayName("Löst");
		when(metadataServiceMock.createResolutionCode(NAMESPACE, MUNICIPALITY_ID, outcome)).thenReturn(ID);

		// Act & Verify
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(outcome)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/resolutioncodes/" + ID)
			.expectBody().isEmpty();

		verify(metadataServiceMock).createResolutionCode(NAMESPACE, MUNICIPALITY_ID, outcome);
	}

	@Test
	void createResolutionCodeWithoutName() {

		// Act & Verify
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(ResolutionCode.create().withDisplayName("Löst"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void getResolutionCode() {

		// Arrange
		final var outcome = ResolutionCode.create().withId(ID).withName("SOLVED");
		when(metadataServiceMock.getResolutionCode(NAMESPACE, MUNICIPALITY_ID, ID)).thenReturn(outcome);

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/{id}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(ResolutionCode.class)
			.returnResult()
			.getResponseBody();

		// Verify
		assertThat(response).isEqualTo(outcome);
		verify(metadataServiceMock).getResolutionCode(NAMESPACE, MUNICIPALITY_ID, ID);
	}

	@Test
	void getResolutionCodes() {

		// Arrange
		when(metadataServiceMock.findResolutionCodes(any(), any(), any(Sort.class)))
			.thenReturn(List.of(ResolutionCode.create().withId(ID).withName("SOLVED")));

		// Act
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(ResolutionCode.class)
			.returnResult()
			.getResponseBody();

		// Verify
		assertThat(response).hasSize(1);
		verify(metadataServiceMock).findResolutionCodes(any(), any(), any(Sort.class));
	}

	@Test
	void updateResolutionCode() {

		// Arrange
		final var outcome = ResolutionCode.create().withName("SOLVED").withDeprecated(true);
		when(metadataServiceMock.updateResolutionCode(NAMESPACE, MUNICIPALITY_ID, ID, outcome)).thenReturn(outcome.withId(ID));

		// Act & Verify
		webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(outcome)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(ResolutionCode.class);

		verify(metadataServiceMock).updateResolutionCode(NAMESPACE, MUNICIPALITY_ID, ID, outcome);
	}

	@Test
	void deleteResolutionCode() {

		// Act & Verify
		webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{id}").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().contentType(ALL)
			.expectBody().isEmpty();

		verify(metadataServiceMock).deleteResolutionCode(NAMESPACE, MUNICIPALITY_ID, ID);
	}

	@Test
	void updateResolutionCodeWithoutName() {

		// Arrange
		final var code = ResolutionCode.create().withDescription("Ärendet är löst").withSortOrder(2);
		when(metadataServiceMock.updateResolutionCode(NAMESPACE, MUNICIPALITY_ID, ID, code)).thenReturn(code.withId(ID).withName("SOLVED"));

		// Act & Verify
		webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(code)
			.exchange()
			.expectStatus().isOk();

		verify(metadataServiceMock).updateResolutionCode(NAMESPACE, MUNICIPALITY_ID, ID, code);
	}

	@Test
	void createResolutionCodeWithTooLongDescription() {

		// Act & Verify
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(ResolutionCode.create().withName("SOLVED").withDescription("a".repeat(1025)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void updateResolutionCodeWithTooLongDescription() {

		// Act & Verify
		webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(ResolutionCode.create().withDescription("a".repeat(1025)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(metadataServiceMock);
	}
}

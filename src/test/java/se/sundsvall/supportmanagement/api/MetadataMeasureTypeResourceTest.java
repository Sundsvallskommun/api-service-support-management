package se.sundsvall.supportmanagement.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.MeasureType;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataMeasureTypeResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/measuretypes";

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createMeasureType() {
		// Setup
		final var id = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
		final var measureType = MeasureType.create().withName("INTERVENTION").withMeasureGroup("MANAGERS");

		// Mock
		when(metadataServiceMock.createMeasureType(NAMESPACE, MUNICIPALITY_ID, measureType)).thenReturn(id);

		// Call
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(measureType)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/measuretypes/" + id)
			.expectBody().isEmpty();

		// Verifications & assertions
		verify(metadataServiceMock).createMeasureType(NAMESPACE, MUNICIPALITY_ID, measureType);
	}

	@Test
	void getMeasureType() {
		// Setup
		final var id = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
		final var measureType = MeasureType.create().withId(id).withName("INTERVENTION").withMeasureGroup("MANAGERS");

		// Mock
		when(metadataServiceMock.getMeasureType(NAMESPACE, MUNICIPALITY_ID, id)).thenReturn(measureType);

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", id)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(MeasureType.class)
			.returnResult()
			.getResponseBody();

		// Verifications & assertions
		verify(metadataServiceMock).getMeasureType(NAMESPACE, MUNICIPALITY_ID, id);
		assertThat(response).isNotNull().isEqualTo(measureType);
	}

	@Test
	void getMeasureTypes() {
		// Call
		webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String[].class)
			.isEqualTo(EMPTY_STRING_ARRAY);

		// Verifications & assertions
		verify(metadataServiceMock).findMeasureTypes(eq(NAMESPACE), eq(MUNICIPALITY_ID), isNull(), any(Sort.class));
	}

	@Test
	void getMeasureTypesWithGroupFilter() {
		// Setup
		final var measureGroup = "MANAGERS";

		// Call
		webTestClient.get().uri(builder -> builder.path(PATH).queryParam("measureGroup", measureGroup).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String[].class)
			.isEqualTo(EMPTY_STRING_ARRAY);

		// Verifications & assertions
		verify(metadataServiceMock).findMeasureTypes(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(measureGroup), any(Sort.class));
	}

	@Test
	void deleteMeasureType() {
		// Setup
		final var id = "5f79a808-0ef3-4985-99b9-b12f23e202a7";

		// Call
		webTestClient.delete().uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", id)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

		// Verifications & assertions
		verify(metadataServiceMock).deleteMeasureType(NAMESPACE, MUNICIPALITY_ID, id);
	}

	@Test
	void updateMeasureType() {
		// Setup
		final var id = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
		final var body = MeasureType.create().withName("INTERVENTION").withMeasureGroup("MANAGERS");

		// Mock
		when(metadataServiceMock.updateMeasureType(NAMESPACE, MUNICIPALITY_ID, id, body)).thenReturn(body);

		// Call
		final var response = webTestClient.patch().uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", id)))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.OK)
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(MeasureType.class)
			.returnResult()
			.getResponseBody();

		// Verifications & assertions
		verify(metadataServiceMock).updateMeasureType(NAMESPACE, MUNICIPALITY_ID, id, body);
		assertThat(response).isNotNull().isEqualTo(body);
	}

	@Test
	void updateMeasureTypeWithoutName() {
		// Setup
		final var id = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
		final var body = MeasureType.create().withDisplayName("New label");

		// Mock
		when(metadataServiceMock.updateMeasureType(NAMESPACE, MUNICIPALITY_ID, id, body)).thenReturn(body);

		// Call
		webTestClient.patch().uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", id)))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isOk();

		// Verifications & assertions
		verify(metadataServiceMock).updateMeasureType(NAMESPACE, MUNICIPALITY_ID, id, body);
	}
}

package se.sundsvall.supportmanagement.api;

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
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataCategoriesResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/categories";

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createCategory() {
		// Parameters
		final var body = Category.create().withName("name");

		// Mock
		when(metadataServiceMock.createCategory(NAMESPACE, MUNICIPALITY_ID, body)).thenReturn(body.getName());

		// Call
		webTestClient.post().uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/categories/" + body.getName())
			.expectBody().isEmpty();

		verify(metadataServiceMock).createCategory(NAMESPACE, MUNICIPALITY_ID, body);
	}

	@Test
	void getCategory() {
		// Parameters
		final var id = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
		final var body = Category.create().withId(id).withName("name");

		// Mock
		when(metadataServiceMock.getCategory(NAMESPACE, MUNICIPALITY_ID, id)).thenReturn(body);

		// Call
		final var result = webTestClient.get().uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", id)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Category.class)
			.isEqualTo(body)
			.returnResult()
			.getResponseBody();

		verify(metadataServiceMock).getCategory(NAMESPACE, MUNICIPALITY_ID, id);
		assertThat(result).isNotNull().isEqualTo(body);
	}

	@Test
	void getCategories() {

		webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String[].class)
			.isEqualTo(EMPTY_STRING_ARRAY);

		verify(metadataServiceMock).findCategories(NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void getCategoryTypes() {
		// Parameters
		final var id = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
		webTestClient.get().uri(builder -> builder.path(PATH + "/{id}/types").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", id)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String[].class)
			.isEqualTo(EMPTY_STRING_ARRAY);

		verify(metadataServiceMock).findTypesByCategoryId(NAMESPACE, MUNICIPALITY_ID, id);
	}

	@Test
	void updateCategory() {
		// Parameters
		final var id = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
		final var body = Category.create().withName("name");

		// Mock
		when(metadataServiceMock.updateCategory(NAMESPACE, MUNICIPALITY_ID, id, body)).thenReturn(body);

		// Call
		webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", id)))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.OK);

		verify(metadataServiceMock).updateCategory(NAMESPACE, MUNICIPALITY_ID, id, body);
	}

	@Test
	void deleteCategory() {
		// Parameters
		final var id = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
		// Call
		webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", id)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

		verify(metadataServiceMock).deleteCategory(NAMESPACE, MUNICIPALITY_ID, id);
	}

}

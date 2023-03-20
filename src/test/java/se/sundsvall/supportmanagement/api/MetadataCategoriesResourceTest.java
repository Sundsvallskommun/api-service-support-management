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
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.service.MetadataService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataCategoriesResourceTest {

	@MockBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@LocalServerPort
	private int port;

	@Test
	void createCatgory() {
		// Parameters
		final var body = Category.create().withName("name");
		
		// Call
		webTestClient.post().uri("my.namespace/2281/metadata/categories")
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED);

		// TODO: Verify when service layer is ready
	}

	@Test
	void getCatgories() {

		webTestClient.get().uri("my.namespace/2281/metadata/categories")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String[].class)
			.isEqualTo(EMPTY_STRING_ARRAY);

		verify(metadataServiceMock).findCategories("my.namespace", "2281");
	}

	@Test
	void getCategoryTypes() {

		webTestClient.get().uri("my.namespace/2281/metadata/categories/category-name/types")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String[].class)
			.isEqualTo(EMPTY_STRING_ARRAY);

		verify(metadataServiceMock).findTypes("my.namespace", "2281", "category-name");
	}

	@Test
	void updateCatgory() {
		// Parameters
		final var body = Category.create().withName("name");
		
		// Call
		webTestClient.patch()
			.uri(builder -> builder.path("my.namespace/2281/metadata/categories/category-name").build())
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED);

		// TODO: Verify when service layer is ready
	}
	
	@Test
	void deleteCatgory() {
		// Call
		webTestClient.delete()
			.uri(builder -> builder.path("my.namespace/2281/metadata/categories/category-name").build())
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED);

		// TODO: Verify when service layer is ready
	}
}

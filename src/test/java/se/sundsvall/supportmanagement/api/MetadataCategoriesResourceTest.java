package se.sundsvall.supportmanagement.api;

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

import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

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
	void createCategory() {
		// Parameters
		final var body = Category.create().withName("name");

		//Mock
		when(metadataServiceMock.createCategory("my.namespace", "2281", body)).thenReturn(body.getName());
		
		// Call
		webTestClient.post().uri("my.namespace/2281/metadata/categories")
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.CREATED);

		verify(metadataServiceMock).createCategory("my.namespace", "2281", body);
	}

	@Test
	void getCategories() {

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
	void updateCategory() {
		// Parameters
		final var body = Category.create().withName("name");

		//Mock
		when(metadataServiceMock.updateCategory("my.namespace", "2281", "category-name", body)).thenReturn(body);

		// Call
		webTestClient.patch()
			.uri(builder -> builder.path("my.namespace/2281/metadata/categories/category-name").build())
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.OK);

		verify(metadataServiceMock).updateCategory("my.namespace", "2281", "category-name", body);
	}
	
	@Test
	void deleteCategory() {
		// Call
		webTestClient.delete()
			.uri(builder -> builder.path("my.namespace/2281/metadata/categories/category-name").build())
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

		verify(metadataServiceMock).deleteCategory("my.namespace", "2281", "category-name");
	}
}

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

import java.util.Map;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataCategoriesResourceTest {

	private static final String PATH = "/{namespace}/{municipalityId}/metadata/categories";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
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
		when(metadataServiceMock.createCategory(NAMESPACE, MUNICIPALITY_ID, body)).thenReturn(body.getName());
		
		// Call
		webTestClient.post().uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("http://localhost:".concat(String.valueOf(port)).concat(fromPath(PATH + "/{category}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "category", body.getName())).toString()))
			.expectBody().isEmpty();

		verify(metadataServiceMock).createCategory(NAMESPACE, MUNICIPALITY_ID, body);
	}

	@Test
	void getCategory() {
		// Parameters
		final var name = "name";
		final var body = Category.create().withName(name);

		//Mock
		when(metadataServiceMock.getCategory(NAMESPACE, MUNICIPALITY_ID, name)).thenReturn(body);

		// Call
		final var result = webTestClient.get().uri(builder -> builder.path(PATH + "/{category}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "category", name)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Category.class)
			.isEqualTo(body)
			.returnResult()
			.getResponseBody();

		verify(metadataServiceMock).getCategory(NAMESPACE, MUNICIPALITY_ID, name);
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
        //Parameters
		final var name = "name";
		webTestClient.get().uri(builder -> builder.path(PATH + "/{category}/types").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "category", name)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String[].class)
			.isEqualTo(EMPTY_STRING_ARRAY);

		verify(metadataServiceMock).findTypes(NAMESPACE, MUNICIPALITY_ID, name);
	}

	@Test
	void updateCategory() {
		// Parameters
		final var name = "name";
		final var body = Category.create().withName("name");

		//Mock
		when(metadataServiceMock.updateCategory(NAMESPACE, MUNICIPALITY_ID, name, body)).thenReturn(body);

		// Call
		webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{category}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "category", name)))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.OK);

		verify(metadataServiceMock).updateCategory(NAMESPACE, MUNICIPALITY_ID, name, body);
	}
	
	@Test
	void deleteCategory() {
		//Parameters
		final var name = "name";
		// Call
		webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{category}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID,   "category", name)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

		verify(metadataServiceMock).deleteCategory(NAMESPACE, MUNICIPALITY_ID, name);
	}
}

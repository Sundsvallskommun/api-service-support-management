package se.sundsvall.supportmanagement.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService;
import se.sundsvall.supportmanagement.service.ErrandParameterService;
import tools.jackson.databind.node.JsonNodeFactory;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandJsonParameterResourceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String KEY = "formData";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/json-parameters";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandJsonParameterService errandJsonParameterServiceMock;

	@MockitoBean
	private ErrandParameterService errandParameterServiceMock;

	@Test
	void readJsonParameter() {
		final var result = JsonParameter.create()
			.withKey(KEY)
			.withSchemaId("test-schema-1.0")
			.withValue(JsonNodeFactory.instance.objectNode().put("name", "test"))
			.withVersion(3L);

		when(errandJsonParameterServiceMock.readJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, KEY)).thenReturn(result);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/{key}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "key", KEY)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectHeader().valueEquals("ETag", "\"3\"")
			.expectBody(JsonParameter.class)
			.returnResult();

		assertThat(response.getResponseBody()).isNotNull();
		assertThat(response.getResponseBody().getKey()).isEqualTo(KEY);
		assertThat(response.getResponseBody().getVersion()).isEqualTo(3L);

		verify(errandJsonParameterServiceMock).readJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, KEY);
		verifyNoMoreInteractions(errandJsonParameterServiceMock);
	}

	@Test
	void updateJsonParameter() {
		final var requestBody = JsonParameter.create()
			.withKey(KEY)
			.withSchemaId("test-schema-1.0")
			.withValue(JsonNodeFactory.instance.objectNode().put("name", "test"));
		final var updated = JsonParameter.create()
			.withKey(KEY)
			.withSchemaId("test-schema-1.0")
			.withValue(JsonNodeFactory.instance.objectNode().put("name", "test"))
			.withVersion(1L);

		when(errandJsonParameterServiceMock.updateJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, KEY, null, requestBody)).thenReturn(updated);

		final var response = webTestClient.put()
			.uri(builder -> builder.path(PATH + "/{key}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "key", KEY)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectHeader().valueEquals("ETag", "\"1\"")
			.expectBody(JsonParameter.class)
			.returnResult();

		assertThat(response.getResponseBody()).isNotNull();
		assertThat(response.getResponseBody().getVersion()).isEqualTo(1L);

		verify(errandJsonParameterServiceMock).updateJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, KEY, null, requestBody);
		verifyNoMoreInteractions(errandJsonParameterServiceMock);
	}

	@Test
	void deleteJsonParameter() {
		webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{key}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "key", KEY)))
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().contentType(ALL_VALUE);

		verify(errandJsonParameterServiceMock).deleteJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, KEY, null);
		verifyNoMoreInteractions(errandJsonParameterServiceMock);
	}
}

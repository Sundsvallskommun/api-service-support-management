package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.ErrandParameterService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandParameterResourceTest {


	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String ERRAND_ID = UUID.randomUUID().toString();

	private static final String PARAMETER_KEY = "parameterKey";

	private static final String PATH = "/{namespace}/{municipalityId}/errands/{errandId}/parameters";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private ErrandParameterService errandParameterServiceMock;

	@Test
	void updateErrandParameters() {
		final var requestBody = Map.of("key", List.of("value"));

		when(errandParameterServiceMock.updateErrandParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, requestBody)).thenReturn(requestBody);

		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(new ParameterizedTypeReference<Map<String, List<String>>>() {

			})
			.returnResult();

		assertThat(response).isNotNull();
		assertThat(response.getResponseBody()).satisfies(p -> {
			assertThat(p).isNotNull();
			assertThat(p).hasSize(1);
			assertThat(p).containsEntry("key", List.of("value"));
		});
		verify(errandParameterServiceMock).updateErrandParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, requestBody);
		verifyNoMoreInteractions(errandParameterServiceMock);
	}

	@Test
	void readErrandParameter() {
		final var errandParameter = List.of("value", "value2");
		when(errandParameterServiceMock.readErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_KEY)).thenReturn(errandParameter);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("/{parameterKey}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "parameterKey", PARAMETER_KEY)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(String.class)
			.returnResult();


		assertThat(response).isNotNull();
		assertThat(response.getResponseBody()).satisfies(p -> {
			assertThat(p).isNotNull();
			assertThat(p).hasSize(1);
			assertThat(p).isEqualTo(List.of("[ \"value\", \"value2\" ]"));
		});

		verify(errandParameterServiceMock).readErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_KEY);
		verifyNoMoreInteractions(errandParameterServiceMock);
	}

	@Test
	void findErrandParameters() {
		final var errandParameters = Map.of("key", List.of("value", "value2"), "key2", List.of("value3"));

		when(errandParameterServiceMock.findErrandParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(errandParameters);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(new ParameterizedTypeReference<Map<String, List<String>>>() {

			})
			.returnResult();

		assertThat(response).isNotNull();
		assertThat(response.getResponseBody()).allSatisfy((k, p) -> {
			assertThat(p).isNotNull();
			if (k.equals("name")) {
				assertThat(p).hasSize(2);
				assertThat(p).contains("value", "value2");
			} else if (k.equals("name2")) {
				assertThat(p).hasSize(1);
				assertThat(p).contains("value3");
			}
		});

		verify(errandParameterServiceMock).findErrandParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		verifyNoMoreInteractions(errandParameterServiceMock);
	}

	@Test
	void updateErrandParameter() {
		final var requestBody = List.of("value");

		final var errandParameter = Map.of("key", List.of("value"));

		when(errandParameterServiceMock.updateErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_KEY, requestBody)).thenReturn(errandParameter.get("key"));

		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH.concat("/{parameterKey}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "parameterKey", PARAMETER_KEY)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(new ParameterizedTypeReference<List<String>>() {

			})
			.returnResult();

		assertThat(response).isNotNull();
		assertThat(response.getResponseBody()).satisfies(p -> {
			assertThat(p).isNotNull();
			assertThat(p).hasSize(1);
			assertThat(p.getFirst()).isEqualTo("value");
		});
		verify(errandParameterServiceMock).updateErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_KEY, requestBody);
		verifyNoMoreInteractions(errandParameterServiceMock);
	}

	@Test
	void deleteErrandParameter() {
		webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("/{parameterKey}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "parameterKey", PARAMETER_KEY)))
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().doesNotExist(CONTENT_TYPE);

		// Verification
		verify(errandParameterServiceMock).deleteErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_KEY);

	}

}

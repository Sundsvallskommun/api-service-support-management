package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.parameter.ErrandParameter;
import se.sundsvall.supportmanagement.api.model.parameter.ErrandParameters;
import se.sundsvall.supportmanagement.service.ErrandParameterService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandParameterResourceTest {


	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = UUID.randomUUID().toString();
	private static final String PARAMETER_ID = UUID.randomUUID().toString();
	private static final String PATH = "/{namespace}/{municipalityId}/errands/{errandId}/parameters";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private ErrandParameterService errandParameterServiceMock;

	@Test
	void createErrandParameter() {
		final var requestBody = ErrandParameter.create()
			.withName("name")
			.withValue("value");

		when(errandParameterServiceMock.createErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, requestBody)).thenReturn(PARAMETER_ID);

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + NAMESPACE + "/" + MUNICIPALITY_ID + "/errands/" + ERRAND_ID + "/parameters/" + PARAMETER_ID)
			.expectBody().isEmpty();

		assertThat(response).isNotNull();
		verify(errandParameterServiceMock).createErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, requestBody);
		verifyNoMoreInteractions(errandParameterServiceMock);
	}

	@Test
	void readErrandParameter() {
		final var errandParameter = ErrandParameter.create()
			.withId(PARAMETER_ID)
			.withName("name")
			.withValue("value");
		when(errandParameterServiceMock.readErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_ID)).thenReturn(errandParameter);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "parameterId", PARAMETER_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(ErrandParameter.class)
			.returnResult();

		assertThat(response).isNotNull();
		assertThat(response.getResponseBody()).satisfies(p -> {
			assertThat(p).isNotNull();
			assertThat(p.getId()).isEqualTo(PARAMETER_ID);
			assertThat(p.getName()).isEqualTo("name");
			assertThat(p.getValue()).isEqualTo("value");
		});

		verify(errandParameterServiceMock).readErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_ID);
		verifyNoMoreInteractions(errandParameterServiceMock);
	}

	@Test
	void findErrandParameters() {
		final var errandParameters = ErrandParameters.create()
			.withErrandParameters(List.of(ErrandParameter.create()
				.withId(PARAMETER_ID)
				.withName("name")
				.withValue("value")));

		when(errandParameterServiceMock.findErrandParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(errandParameters);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(ErrandParameters.class)
			.returnResult();

		assertThat(response).isNotNull();
		assertThat(response.getResponseBody().getParameters()).allSatisfy(p -> {
			assertThat(p).isNotNull();
			assertThat(p.getId()).isEqualTo(PARAMETER_ID);
			assertThat(p.getName()).isEqualTo("name");
			assertThat(p.getValue()).isEqualTo("value");
		});

		verify(errandParameterServiceMock).findErrandParameters(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		verifyNoMoreInteractions(errandParameterServiceMock);
	}

	@Test
	void updateErrandParameter() {
		final var requestBody = ErrandParameter.create()
			.withName("name")
			.withValue("value");

		var errandParameter = ErrandParameter.create()
			.withId(PARAMETER_ID)
			.withValue("value")
			.withName("name");

		when(errandParameterServiceMock.updateErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_ID, requestBody)).thenReturn(errandParameter);

		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "parameterId", PARAMETER_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(ErrandParameter.class)
			.returnResult();

		assertThat(response).isNotNull();
		assertThat(response.getResponseBody()).satisfies(p -> {
			assertThat(p).isNotNull();
			assertThat(p.getId()).isEqualTo(PARAMETER_ID);
			assertThat(p.getName()).isEqualTo("name");
			assertThat(p.getValue()).isEqualTo("value");
		});
		verify(errandParameterServiceMock).updateErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_ID, requestBody);
		verifyNoMoreInteractions(errandParameterServiceMock);
	}

	@Test
	void deleteErrandParameter() {
		webTestClient.delete()
			.uri(builder -> builder.path(PATH.concat("/{parameterId}")).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "parameterId", PARAMETER_ID)))
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().doesNotExist(CONTENT_TYPE);

		// Verification
		verify(errandParameterServiceMock).deleteErrandParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, PARAMETER_ID);

	}

}

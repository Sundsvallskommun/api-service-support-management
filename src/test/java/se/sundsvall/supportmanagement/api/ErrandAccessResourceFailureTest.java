package se.sundsvall.supportmanagement.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.ErrandAccessService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandAccessResourceFailureTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String INVALID = "#invalid#";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/access";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandAccessService errandAccessServiceMock;

	private ConstraintViolationProblem readWithInvalid(final Map<String, String> pathVariables) {
		return webTestClient.get()
			.uri(builder -> builder.path(PATH).build(pathVariables))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();
	}

	@Test
	void readErrandAccessInvalidNamespace() {

		final var response = readWithInvalid(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID));

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(tuple("readErrandAccess.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(errandAccessServiceMock);
	}

	@Test
	void readErrandAccessInvalidMunicipalityId() {

		final var response = readWithInvalid(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID));

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(tuple("readErrandAccess.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(errandAccessServiceMock);
	}

	@Test
	void readErrandAccessInvalidErrandId() {

		final var response = readWithInvalid(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", INVALID));

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(tuple("readErrandAccess.errandId", "not a valid UUID"));

		verifyNoInteractions(errandAccessServiceMock);
	}
}

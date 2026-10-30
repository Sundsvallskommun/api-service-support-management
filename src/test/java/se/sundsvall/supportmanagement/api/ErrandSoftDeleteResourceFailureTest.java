package se.sundsvall.supportmanagement.api;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.supportmanagement.service.ErrandService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResourceTest
class ErrandSoftDeleteResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = UUID.randomUUID().toString();
	private static final String INVALID = "invalid.value";

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private ErrandService errandServiceMock;

	@ParameterizedTest
	@CsvSource({
		"soft-delete, softDeleteErrand",
		"restore, restoreErrand"
	})
	void withInvalidNamespace(final String operation, final String methodName) {
		final var response = post(operation, Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID));

		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple(methodName + ".namespace", "can only contain A-Z, a-z, 0-9, - and _"));
		verifyNoInteractions(errandServiceMock);
	}

	@ParameterizedTest
	@CsvSource({
		"soft-delete, softDeleteErrand",
		"restore, restoreErrand"
	})
	void withInvalidMunicipalityId(final String operation, final String methodName) {
		final var response = post(operation, Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID));

		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple(methodName + ".municipalityId", "not a valid municipality ID"));
		verifyNoInteractions(errandServiceMock);
	}

	@ParameterizedTest
	@CsvSource({
		"soft-delete, softDeleteErrand",
		"restore, restoreErrand"
	})
	void withInvalidErrandId(final String operation, final String methodName) {
		final var response = post(operation, Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", INVALID));

		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple(methodName + ".errandId", "not a valid UUID"));
		verifyNoInteractions(errandServiceMock);
	}

	private ConstraintViolationProblem post(final String operation, final Map<String, String> parameters) {
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH + "/" + operation).build(parameters))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		return response;
	}
}

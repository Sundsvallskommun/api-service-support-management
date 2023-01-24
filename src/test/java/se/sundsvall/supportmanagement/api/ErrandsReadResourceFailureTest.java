package se.sundsvall.supportmanagement.api;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.ErrandService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandsReadResourceFailureTest {

	private static final String PATH = "/errands";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private ErrandService errandServiceMock;

	@Test
	void readErrandWithInvalidUuid() {
		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/{id}").build(Map.of("id", "invalid-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("readErrand.id", "not a valid UUID"));

		// Verification
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void findErrandsWithInvalidFilterString() {
		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH).queryParam("filter", "categoryTag:'SUPPORT_CASE' and").build(emptyMap()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Invalid Filter Content");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getDetail()).isEqualTo("mismatched input '<EOF>' expecting {NOT, TRUE, FALSE, '(', ID, NUMBER, STRING}");

		// Verification
		verifyNoInteractions(errandServiceMock);
	}
}

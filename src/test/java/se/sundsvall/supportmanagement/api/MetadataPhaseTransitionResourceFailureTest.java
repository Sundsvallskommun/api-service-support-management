package se.sundsvall.supportmanagement.api;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.PhaseTransition;
import se.sundsvall.supportmanagement.service.MetadataService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataPhaseTransitionResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/phases/{phaseId}/transitions";

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static Stream<Arguments> createTransitionSingleViolationProvider() {
		final var validPhaseId = randomUUID().toString();
		return Stream.of(
			Arguments.of(PhaseTransition.create().withTargetPhaseId("not-a-uuid"), "2281", "namespace", validPhaseId, "targetPhaseId", "not a valid UUID"),
			Arguments.of(PhaseTransition.create().withTargetPhaseId(randomUUID().toString()), "2281", "#not-a-valid-namespace", validPhaseId, "createTransition.namespace", "can only contain A-Z, a-z, 0-9, - and _"),
			Arguments.of(PhaseTransition.create().withTargetPhaseId(randomUUID().toString()), "not-a-valid-municipalityId", "namespace", validPhaseId, "createTransition.municipalityId", "not a valid municipality ID"),
			Arguments.of(PhaseTransition.create().withTargetPhaseId(randomUUID().toString()), "2281", "namespace", "not-a-valid-uuid", "createTransition.phaseId", "not a valid UUID"));
	}

	@ParameterizedTest
	@MethodSource("createTransitionSingleViolationProvider")
	void createTransitionSingleViolation(PhaseTransition transition, String municipalityId, String namespace, String phaseId, String field, String message) {
		final var response = webTestClient.post().uri(builder -> builder.path(PATH).build(municipalityId, namespace, phaseId))
			.contentType(APPLICATION_JSON)
			.bodyValue(transition)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactly(tuple(field, message));

		verifyNoInteractions(metadataServiceMock);
	}

	private static Stream<Arguments> createTransitionMultiViolationProvider() {
		final var validPhaseId = randomUUID().toString();
		return Stream.of(
			Arguments.of(PhaseTransition.create().withTargetPhaseId(null), "2281", "namespace", validPhaseId),
			Arguments.of(PhaseTransition.create().withTargetPhaseId(""), "2281", "namespace", validPhaseId));
	}

	@ParameterizedTest
	@MethodSource("createTransitionMultiViolationProvider")
	void createTransitionMultiViolation(PhaseTransition transition, String municipalityId, String namespace, String phaseId) {
		final var response = webTestClient.post().uri(builder -> builder.path(PATH).build(municipalityId, namespace, phaseId))
			.contentType(APPLICATION_JSON)
			.bodyValue(transition)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(tuple("targetPhaseId", "must not be blank"), tuple("targetPhaseId", "not a valid UUID"));

		verifyNoInteractions(metadataServiceMock);
	}

	private static Stream<Arguments> deleteTransitionInvalidParamsProvider() {
		final var validPhaseId = randomUUID().toString();
		final var validTransitionId = randomUUID().toString();
		return Stream.of(
			Arguments.of("2281", "namespace", "not-a-valid-uuid", validTransitionId, "deleteTransition.phaseId", "not a valid UUID"),
			Arguments.of("2281", "namespace", validPhaseId, "not-a-valid-uuid", "deleteTransition.transitionId", "not a valid UUID"),
			Arguments.of("2281", "#not-a-valid-namespace", validPhaseId, validTransitionId, "deleteTransition.namespace", "can only contain A-Z, a-z, 0-9, - and _"),
			Arguments.of("not-a-valid-municipalityId", "namespace", validPhaseId, validTransitionId, "deleteTransition.municipalityId", "not a valid municipality ID"));
	}

	@ParameterizedTest
	@MethodSource("deleteTransitionInvalidParamsProvider")
	void deleteTransitionWithInvalidParams(String municipalityId, String namespace, String phaseId, String transitionId, String field, String message) {
		final var response = webTestClient.delete().uri(builder -> builder.path(PATH + "/{transitionId}").build(municipalityId, namespace, phaseId, transitionId))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactly(tuple(field, message));

		verifyNoInteractions(metadataServiceMock);
	}
}

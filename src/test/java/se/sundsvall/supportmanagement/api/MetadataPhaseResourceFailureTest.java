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
import se.sundsvall.supportmanagement.api.model.metadata.Phase;
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
class MetadataPhaseResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/phases";

	private static final String TRANSITION_PATH = PATH + "/{phaseId}/transitions";

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	// =================================================================
	// Phase failure tests
	// =================================================================

	private static Stream<Arguments> createPhaseArgumentProvider() {
		return Stream.of(
			Arguments.of(Phase.create().withName(""), "2281", "namespace", "name", "must not be blank"),
			Arguments.of(Phase.create().withName(null), "2281", "namespace", "name", "must not be blank"),
			Arguments.of(Phase.create().withName("INVESTIGATION"), "2281", "#not-a-valid-namespace", "createPhase.namespace", "can only contain A-Z, a-z, 0-9, - and _"),
			Arguments.of(Phase.create().withName("INVESTIGATION"), "not-a-valid-municipalityId", "namespace", "createPhase.municipalityId", "not a valid municipality ID"));
	}

	@ParameterizedTest
	@MethodSource("createPhaseArgumentProvider")
	void createPhase(Phase phase, String municipalityId, String namespace, String field, String message) {
		final var response = webTestClient.post().uri(builder -> builder.path(PATH).build(municipalityId, namespace))
			.contentType(APPLICATION_JSON)
			.bodyValue(phase)
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

	private static Stream<Arguments> getPhasesInvalidParamsProvider() {
		return Stream.of(
			Arguments.of("not-a-valid-municipalityId", "namespace", "getPhases.municipalityId", "not a valid municipality ID"),
			Arguments.of("2281", "#not-a-valid-namespace", "getPhases.namespace", "can only contain A-Z, a-z, 0-9, - and _"));
	}

	@ParameterizedTest
	@MethodSource("getPhasesInvalidParamsProvider")
	void getPhasesWithInvalidParams(String municipalityId, String namespace, String field, String message) {
		final var response = webTestClient.get().uri(builder -> builder.path(PATH).build(municipalityId, namespace))
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

	private static Stream<Arguments> getPhaseInvalidPhaseIdProvider() {
		return Stream.of(
			Arguments.of("2281", "namespace", "not-a-valid-uuid", "getPhase.phaseId", "not a valid UUID"),
			Arguments.of("2281", "#not-a-valid-namespace", randomUUID().toString(), "getPhase.namespace", "can only contain A-Z, a-z, 0-9, - and _"),
			Arguments.of("not-a-valid-municipalityId", "namespace", randomUUID().toString(), "getPhase.municipalityId", "not a valid municipality ID"));
	}

	@ParameterizedTest
	@MethodSource("getPhaseInvalidPhaseIdProvider")
	void getPhaseWithInvalidParams(String municipalityId, String namespace, String phaseId, String field, String message) {
		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/{phaseId}").build(municipalityId, namespace, phaseId))
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

	private static Stream<Arguments> updatePhaseInvalidParamsProvider() {
		return Stream.of(
			Arguments.of("not-a-valid-municipalityId", "namespace", randomUUID().toString(), "updatePhase.municipalityId", "not a valid municipality ID"),
			Arguments.of("2281", "#not-a-valid-namespace", randomUUID().toString(), "updatePhase.namespace", "can only contain A-Z, a-z, 0-9, - and _"),
			Arguments.of("2281", "namespace", "not-a-valid-uuid", "updatePhase.phaseId", "not a valid UUID"));
	}

	@ParameterizedTest
	@MethodSource("updatePhaseInvalidParamsProvider")
	void updatePhaseWithInvalidParams(String municipalityId, String namespace, String phaseId, String field, String message) {
		final var response = webTestClient.patch().uri(builder -> builder.path(PATH + "/{phaseId}").build(municipalityId, namespace, phaseId))
			.contentType(APPLICATION_JSON)
			.bodyValue(Phase.create().withName("INVESTIGATION"))
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

	private static Stream<Arguments> deletePhaseInvalidPhaseIdProvider() {
		return Stream.of(
			Arguments.of("2281", "namespace", "not-a-valid-uuid", "deletePhase.phaseId", "not a valid UUID"),
			Arguments.of("2281", "#not-a-valid-namespace", randomUUID().toString(), "deletePhase.namespace", "can only contain A-Z, a-z, 0-9, - and _"),
			Arguments.of("not-a-valid-municipalityId", "namespace", randomUUID().toString(), "deletePhase.municipalityId", "not a valid municipality ID"));
	}

	@ParameterizedTest
	@MethodSource("deletePhaseInvalidPhaseIdProvider")
	void deletePhaseWithInvalidParams(String municipalityId, String namespace, String phaseId, String field, String message) {
		final var response = webTestClient.delete().uri(builder -> builder.path(PATH + "/{phaseId}").build(municipalityId, namespace, phaseId))
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

	// =================================================================
	// Phase Transition failure tests
	// =================================================================

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
		final var response = webTestClient.post().uri(builder -> builder.path(TRANSITION_PATH).build(municipalityId, namespace, phaseId))
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
		final var response = webTestClient.post().uri(builder -> builder.path(TRANSITION_PATH).build(municipalityId, namespace, phaseId))
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

	private static Stream<Arguments> getTransitionsInvalidParamsProvider() {
		final var validPhaseId = randomUUID().toString();
		return Stream.of(
			Arguments.of("not-a-valid-municipalityId", "namespace", validPhaseId, "getTransitions.municipalityId", "not a valid municipality ID"),
			Arguments.of("2281", "#not-a-valid-namespace", validPhaseId, "getTransitions.namespace", "can only contain A-Z, a-z, 0-9, - and _"),
			Arguments.of("2281", "namespace", "not-a-valid-uuid", "getTransitions.phaseId", "not a valid UUID"));
	}

	@ParameterizedTest
	@MethodSource("getTransitionsInvalidParamsProvider")
	void getTransitionsWithInvalidParams(String municipalityId, String namespace, String phaseId, String field, String message) {
		final var response = webTestClient.get().uri(builder -> builder.path(TRANSITION_PATH).build(municipalityId, namespace, phaseId))
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
		final var response = webTestClient.delete().uri(builder -> builder.path(TRANSITION_PATH + "/{transitionId}").build(municipalityId, namespace, phaseId, transitionId))
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

package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.service.MetadataService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataRoleResourceFailureTest {
	private static final String PATH = "/{namespace}/{municipalityId}/metadata/roles";

	@MockBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@ParameterizedTest
	@MethodSource("createRoleArguments")
	void createWithInvalidArguments(final String namespace, final String municipalityId, final Tuple expectedResponse) {

		final var response = webTestClient.post().uri(builder -> builder.path(PATH).build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
			.bodyValue(Role.create().withName("name"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactly(expectedResponse);

		verifyNoInteractions(metadataServiceMock);
	}

	private static Stream<Arguments> createRoleArguments() {
		return Stream.of(
			Arguments.of("my.namespace", "666", tuple("createRole.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("createRole.namespace", "can only contain A-Z, a-z, 0-9, -, _ and .")));
	}

	@ParameterizedTest
	@MethodSource("getRoleArguments")
	void getRoleWithInvalidArguments(final String namespace, final String municipalityId, final Tuple expectedResponse) {
		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/role").build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactly(expectedResponse);

		verifyNoInteractions(metadataServiceMock);
	}

	private static Stream<Arguments> getRoleArguments() {
		return Stream.of(
			Arguments.of("my.namespace", "666", tuple("getRole.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("getRole.namespace", "can only contain A-Z, a-z, 0-9, -, _ and .")));
	}

	@ParameterizedTest
	@MethodSource("getRolesArguments")
	void getRolesWithInvalidArguments(final String namespace, final String municipalityId, final Tuple expectedResponse) {

		final var response = webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactly(expectedResponse);

		verifyNoInteractions(metadataServiceMock);
	}

	private static Stream<Arguments> getRolesArguments() {
		return Stream.of(
			Arguments.of("my.namespace", "666", tuple("getRoles.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("getRoles.namespace", "can only contain A-Z, a-z, 0-9, -, _ and .")));
	}

	@ParameterizedTest
	@MethodSource("deleteArguments")
	void deleteWithInvalidArguments(final String namespace, final String municipalityId, final Tuple expectedResponse) {

		final var response = webTestClient.delete().uri(builder -> builder.path(PATH + "/role").build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactly(expectedResponse);

		verifyNoInteractions(metadataServiceMock);
	}

	private static Stream<Arguments> deleteArguments() {
		return Stream.of(
			Arguments.of("my.namespace", "666", tuple("deleteRole.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("deleteRole.namespace", "can only contain A-Z, a-z, 0-9, -, _ and .")));
	}
}

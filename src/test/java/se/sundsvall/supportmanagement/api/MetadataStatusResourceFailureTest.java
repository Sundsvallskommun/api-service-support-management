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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.service.MetadataService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataStatusResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/statuses";

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@ParameterizedTest
	@MethodSource("createStatusArguments")
	void createWithInvalidArguments(String namespace, String municipalityId, Tuple expectedResponse) {

		final var response = webTestClient.post().uri(builder -> builder.path(PATH).build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
			.bodyValue(Status.create().withName("name"))
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

	private static Stream<Arguments> createStatusArguments() {
		return Stream.of(
			Arguments.of("my-namespace", "666", tuple("createStatus.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("createStatus.namespace", "can only contain A-Z, a-z, 0-9, - and _")));
	}

	@ParameterizedTest
	@MethodSource("getStatusArguments")
	void getStatusWithInvalidArguments(String namespace, String municipalityId, Tuple expectedResponse) {
		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/status").build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
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

	private static Stream<Arguments> getStatusArguments() {
		return Stream.of(
			Arguments.of("my-namespace", "666", tuple("getStatus.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("getStatus.namespace", "can only contain A-Z, a-z, 0-9, - and _")));
	}

	@ParameterizedTest
	@MethodSource("getStatusesArguments")
	void getStatusesWithInvalidArguments(String namespace, String municipalityId, Tuple expectedResponse) {

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

	private static Stream<Arguments> getStatusesArguments() {
		return Stream.of(
			Arguments.of("my-namespace", "666", tuple("getStatuses.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("getStatuses.namespace", "can only contain A-Z, a-z, 0-9, - and _")));
	}

	@ParameterizedTest
	@MethodSource("deleteArguments")
	void deleteWithInvalidArguments(String namespace, String municipalityId, Tuple expectedResponse) {

		final var response = webTestClient.delete().uri(builder -> builder.path(PATH + "/status").build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
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
			Arguments.of("my-namespace", "666", tuple("deleteStatus.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("deleteStatus.namespace", "can only contain A-Z, a-z, 0-9, - and _")));
	}
}

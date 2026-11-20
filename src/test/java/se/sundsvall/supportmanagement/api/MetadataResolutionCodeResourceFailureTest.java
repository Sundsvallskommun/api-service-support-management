package se.sundsvall.supportmanagement.api;

import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.supportmanagement.api.model.metadata.ResolutionCode;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResourceTest
class MetadataResolutionCodeResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/resolutioncodes";

	@Autowired
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@ParameterizedTest
	@MethodSource("createResolutionCodeArguments")
	void createWithInvalidArguments(final String namespace, final String municipalityId, final Tuple expectedResponse) {

		final var response = webTestClient.post().uri(builder -> builder.path(PATH).build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
			.bodyValue(ResolutionCode.create().withName("name"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactly(expectedResponse);

		verifyNoInteractions(metadataServiceMock);
	}

	private static Stream<Arguments> createResolutionCodeArguments() {
		return Stream.of(
			Arguments.of("MY_NAMESPACE", "666", tuple("createResolutionCode.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("createResolutionCode.namespace", "can only contain A-Z, a-z, 0-9, - and _")));
	}

	@ParameterizedTest
	@MethodSource("getResolutionCodeArguments")
	void getResolutionCodeWithInvalidArguments(final String namespace, final String municipalityId, final Tuple expectedResponse) {
		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", namespace, "municipalityId", municipalityId, "id", "5f79a808-0ef3-4985-99b9-b12f23e202a7")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactly(expectedResponse);

		verifyNoInteractions(metadataServiceMock);
	}

	private static Stream<Arguments> getResolutionCodeArguments() {
		return Stream.of(
			Arguments.of("MY_NAMESPACE", "666", tuple("getResolutionCode.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("getResolutionCode.namespace", "can only contain A-Z, a-z, 0-9, - and _")));
	}

	@ParameterizedTest
	@MethodSource("getResolutionCodesArguments")
	void getResolutionCodesWithInvalidArguments(final String namespace, final String municipalityId, final Tuple expectedResponse) {

		final var response = webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactly(expectedResponse);

		verifyNoInteractions(metadataServiceMock);
	}

	private static Stream<Arguments> getResolutionCodesArguments() {
		return Stream.of(
			Arguments.of("MY_NAMESPACE", "666", tuple("getResolutionCodes.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("getResolutionCodes.namespace", "can only contain A-Z, a-z, 0-9, - and _")));
	}

	@ParameterizedTest
	@MethodSource("deleteArguments")
	void deleteWithInvalidArguments(final String namespace, final String municipalityId, final Tuple expectedResponse) {

		final var response = webTestClient.delete().uri(builder -> builder.path(PATH + "/{id}").build(Map.of("namespace", namespace, "municipalityId", municipalityId, "id", "5f79a808-0ef3-4985-99b9-b12f23e202a7")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactly(expectedResponse);

		verifyNoInteractions(metadataServiceMock);
	}

	private static Stream<Arguments> deleteArguments() {
		return Stream.of(
			Arguments.of("MY_NAMESPACE", "666", tuple("deleteResolutionCode.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("deleteResolutionCode.namespace", "can only contain A-Z, a-z, 0-9, - and _")));
	}
}

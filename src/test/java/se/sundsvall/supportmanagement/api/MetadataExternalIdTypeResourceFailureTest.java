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
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.service.MetadataService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataExternalIdTypeResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/external-id-types";

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@ParameterizedTest
	@MethodSource("createExternalIdTypeArguments")
	void createWithInvalidMunicipalityId(String namespace, String municipalityId, Tuple expectedResponse) {

		final var response = webTestClient.post().uri(builder -> builder.path(PATH).build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
			.bodyValue(ExternalIdType.create().withName("name"))
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

	private static Stream<Arguments> createExternalIdTypeArguments() {
		return Stream.of(
			Arguments.of("MY_NAMESPACE", "666", tuple("createExternalIdType.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("createExternalIdType.namespace", "can only contain A-Z, a-z, 0-9, - and _")));
	}

	@ParameterizedTest
	@MethodSource("getExternalIdTypeArguments")
	void getExternalIdTypeWithInvalidArguments(String namespace, String municipalityId, Tuple expectedResponse) {
		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/externalIdType").build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
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

	private static Stream<Arguments> getExternalIdTypeArguments() {
		return Stream.of(
			Arguments.of("MY_NAMESPACE", "666", tuple("getExternalIdType.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("getExternalIdType.namespace", "can only contain A-Z, a-z, 0-9, - and _")));
	}

	@ParameterizedTest
	@MethodSource("getExternalIdTypesArguments")
	void getExternalIdTypesWithInvalidArguments(String namespace, String municipalityId, Tuple expectedResponse) {
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

	private static Stream<Arguments> getExternalIdTypesArguments() {
		return Stream.of(
			Arguments.of("MY_NAMESPACE", "666", tuple("getExternalIdTypes.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("getExternalIdTypes.namespace", "can only contain A-Z, a-z, 0-9, - and _")));
	}

	@ParameterizedTest
	@MethodSource("deleteArguments")
	void deleteWithInvalidArguments(String namespace, String municipalityId, Tuple expectedResponse) {

		final var response = webTestClient.delete().uri(builder -> builder.path(PATH + "/externalIdType").build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
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
			Arguments.of("MY_NAMESPACE", "666", tuple("deleteExternalIdType.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("deleteExternalIdType.namespace", "can only contain A-Z, a-z, 0-9, - and _")));
	}
}

package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.List;
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
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.service.MetadataService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataLabelResourceFailureTest {

	private static final String PATH = "/{namespace}/{municipalityId}/metadata/labels";

	@MockBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@ParameterizedTest
	@MethodSource("createLabelsArguments")
	void createWithInvalidArguments(final String namespace, final String municipalityId, List<Label> labels, final Tuple expectedResponse) {

		final var response = webTestClient.post().uri(builder -> builder.path(PATH).build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
			.bodyValue(labels)
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

	@ParameterizedTest
	@MethodSource("updateLabelsArguments")
	void updateWithInvalidArguments(final String namespace, final String municipalityId, List<Label> labels, final Tuple expectedResponse) {
		final var response = webTestClient.put().uri(builder -> builder.path(PATH).build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
				.bodyValue(labels)
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

	private static Stream<Arguments> createLabelsArguments() {
		return labelsArguments("createLabels");
	}

	private static Stream<Arguments> updateLabelsArguments() {
		return labelsArguments("updateLabels");
	}

	private static Stream<Arguments> labelsArguments(String method) {
		return Stream.of(
				Arguments.of("my.namespace", "2281", List.of(createLabel("class", "name"), createLabel("class", "name")), tuple(method + ".body", "each entry must have unique name and same classification compared to its siblings")),
				Arguments.of("my.namespace", "2281", List.of(createLabel("class_1", "name_1"), createLabel("class_2", "name_2")), tuple(method + ".body", "each entry must have unique name and same classification compared to its siblings")),
				Arguments.of("my.namespace", "2281", List.of(createLabel("classification", null)), tuple(method + ".body[0].name", "must not be blank")),
				Arguments.of("my.namespace", "2281", List.of(createLabel(null, "name")), tuple(method + ".body[0].classification", "must not be blank")),
				Arguments.of("my.namespace", "666", List.of(createLabel("classification", "name")), tuple(method + ".municipalityId", "not a valid municipality ID")),
				Arguments.of("invalid,namespace", "2281", List.of(createLabel("classification", "name")), tuple(method + ".namespace", "can only contain A-Z, a-z, 0-9, -, _ and .")));
	}

	private static Label createLabel(String classification, String name) {
		return Label.create().withClassification(classification).withName(name);
	}

	@ParameterizedTest
	@MethodSource("getLabelsArguments")
	void getRoleWithInvalidArguments(final String namespace, final String municipalityId, final Tuple expectedResponse) {
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

	private static Stream<Arguments> getLabelsArguments() {
		return Stream.of(
			Arguments.of("my.namespace", "666", tuple("getLabels.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("getLabels.namespace", "can only contain A-Z, a-z, 0-9, -, _ and .")));
	}

	@ParameterizedTest
	@MethodSource("deleteLabelArguments")
	void deleteWithInvalidArguments(final String namespace, final String municipalityId, final Tuple expectedResponse) {

		final var response = webTestClient.delete().uri(builder -> builder.path(PATH).build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
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

	private static Stream<Arguments> deleteLabelArguments() {
		return Stream.of(
			Arguments.of("my.namespace", "666", tuple("deleteLabels.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("deleteLabels.namespace", "can only contain A-Z, a-z, 0-9, -, _ and .")));
	}
}

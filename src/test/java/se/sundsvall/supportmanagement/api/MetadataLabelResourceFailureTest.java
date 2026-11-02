package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.LabelAttribute;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureRequest;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStep;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataLabelResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/labels";

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@ParameterizedTest
	@MethodSource("createLabelsArguments")
	void createWithInvalidArguments(final String namespace, final String municipalityId, List<Label> labels, final Tuple... expectedResponse) {

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
			.bodyValue(labels)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactlyInAnyOrder(expectedResponse);

		verifyNoInteractions(metadataServiceMock);
	}

	@ParameterizedTest
	@MethodSource("updateLabelsArguments")
	void updateWithInvalidArguments(final String namespace, final String municipalityId, List<Label> labels, final Tuple... expectedResponse) {

		final var response = webTestClient.put()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
			.bodyValue(labels)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactlyInAnyOrder(expectedResponse);

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
			Arguments.of("MY_NAMESPACE", "2281", List.of(createLabelWithAttributes("class", "RES",
				LabelAttribute.create().withKey("dup").withValue("a"),
				LabelAttribute.create().withKey("dup").withValue("b"))),
				tuples(
					tuple(method + ".labels", "each label must have unique attribute keys"))),
			Arguments.of("MY_NAMESPACE", "2281", List.of(createLabel("class", "RES").withLabels(List.of(
				createLabelWithAttributes("class", "CHILD",
					LabelAttribute.create().withKey("k").withValue("v1"),
					LabelAttribute.create().withKey("k").withValue("v2"))))),
				tuples(
					tuple(method + ".labels", "each label must have unique attribute keys"))),
			Arguments.of("MY_NAMESPACE", "2281", List.of(createLabel("class", "RESOURCE_NAME_1"), createLabel("class", "RESOURCE_NAME_1")),
				tuples(
					tuple(method + ".labels", "each entry must have unique resourceName compared to its siblings"))),
			Arguments.of("MY_NAMESPACE", "2281", List.of(createLabel("class", "RESOURCE_NAME"), createLabel("class", "RESOURCE_NAME")),
				tuples(
					tuple(method + ".labels", "each entry must have unique resourceName compared to its siblings"))),
			Arguments.of("MY_NAMESPACE", "2281", List.of(createLabel(null, "RESOURCE_NAME")),
				tuples(
					tuple(method + ".labels[0].classification", "must not be blank"))),
			Arguments.of("MY_NAMESPACE", "666", List.of(createLabel("classification", "RESOURCE_NAME")),
				tuples(
					tuple(method + ".municipalityId", "not a valid municipality ID"))),
			Arguments.of("invalid,namespace", "2281", List.of(createLabel("classification", "RESOURCE_NAME")),
				tuples(
					tuple(method + ".namespace", "can only contain A-Z, a-z, 0-9, - and _"))),
			Arguments.of("MY_NAMESPACE", "2281", List.of(createLabel("classification", null)),
				tuples(
					tuple(method + ".labels[0].resourceName", "must not be blank"))),
			Arguments.of("MY_NAMESPACE", "2281", List.of(createLabel("classification", "")),
				tuples(
					tuple(method + ".labels[0].resourceName", "can only contain A-Z, 0-9 and _"),
					tuple(method + ".labels[0].resourceName", "must not be blank"))),
			Arguments.of("MY_NAMESPACE", "2281", List.of(createLabel("classification", "RESOURCE-NAME")),
				tuples(
					tuple(method + ".labels[0].resourceName", "can only contain A-Z, 0-9 and _"))),
			Arguments.of("MY_NAMESPACE", "2281", List.of(createLabel("classification", "RESOURCE_NAME/RESOURCE_1")),
				tuples(
					tuple(method + ".labels[0].resourceName", "can only contain A-Z, 0-9 and _"))),

			Arguments.of("MY_NAMESPACE", "2281", List.of(createLabel("classification", "resourceName")),
				tuples(
					tuple(method + ".labels[0].resourceName", "can only contain A-Z, 0-9 and _"))));

	}

	private static Label createLabel(String classification, String resourceName) {
		return Label.create().withClassification(classification).withResourceName(resourceName);
	}

	private static Label createLabelWithAttributes(String classification, String resourceName, LabelAttribute... attributes) {
		return createLabel(classification, resourceName).withAttributes(List.of(attributes));
	}

	@ParameterizedTest
	@MethodSource("getLabelsArguments")
	void getRoleWithInvalidArguments(final String namespace, final String municipalityId, final Tuple expectedResponse) {

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactlyInAnyOrder(expectedResponse);

		verifyNoInteractions(metadataServiceMock);
	}

	private static Stream<Arguments> getLabelsArguments() {
		return Stream.of(
			Arguments.of("MY_NAMESPACE", "666", tuple("getLabels.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("getLabels.namespace", "can only contain A-Z, a-z, 0-9, - and _")));
	}

	@ParameterizedTest
	@MethodSource("deleteLabelArguments")
	void deleteWithInvalidArguments(final String namespace, final String municipalityId, final Tuple expectedResponse) {

		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactlyInAnyOrder(expectedResponse);

		verifyNoInteractions(metadataServiceMock);
	}

	private static Stream<Arguments> deleteLabelArguments() {
		return Stream.of(
			Arguments.of("MY_NAMESPACE", "666", tuple("deleteLabels.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", tuple("deleteLabels.namespace", "can only contain A-Z, a-z, 0-9, - and _")));
	}

	private static Tuple[] tuples(Tuple... tuples) {
		return tuples;
	}

	@Test
	void restructureLabelsWithMissingDryRun_returns400() {
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH + "/restructure").build(Map.of("namespace", "MY_NAMESPACE", "municipalityId", "2281")))
			.contentType(APPLICATION_JSON)
			.bodyValue(Map.of("steps", List.of()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.contains(tuple("dryRun", "must not be null"));

		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	@DisplayName("Verification that an empty steps list is rejected, since a restructure with nothing to do is meaningless")
	void restructureLabelsWithEmptySteps_returns400() {
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH + "/restructure").build(Map.of("namespace", "MY_NAMESPACE", "municipalityId", "2281")))
			.contentType(APPLICATION_JSON)
			.bodyValue(LabelRestructureRequest.create().withDryRun(true).withSteps(List.of()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.contains(tuple("steps", "must not be empty"));

		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	@DisplayName("Verification that an ADD step missing its required classification is rejected by @ValidRestructureStep before ever reaching the service")
	void restructureLabelsWithInvalidStep_returns400() {
		final var invalidStep = LabelRestructureStep.create().withType(LabelRestructureStepType.ADD).withPath(List.of("CATEGORY", "TYPE")).withDisplayName("Type");

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH + "/restructure").build(Map.of("namespace", "MY_NAMESPACE", "municipalityId", "2281")))
			.contentType(APPLICATION_JSON)
			.bodyValue(LabelRestructureRequest.create().withDryRun(true).withSteps(List.of(invalidStep)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::message)
			.contains("fields present on this step do not match what its type allows");

		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void restructureLabels_stepValidationFailureAtService_returns400() {
		when(metadataServiceMock.restructureLabelTree(eq("MY_NAMESPACE"), eq("2281"), any()))
			.thenThrow(Problem.valueOf(BAD_REQUEST, "Step 0 (MOVE): label at path 'CATEGORY/GONE' does not exist"));

		webTestClient.post()
			.uri(builder -> builder.path(PATH + "/restructure").build(Map.of("namespace", "MY_NAMESPACE", "municipalityId", "2281")))
			.contentType(APPLICATION_JSON)
			.bodyValue(LabelRestructureRequest.create().withDryRun(true)
				.withSteps(List.of(LabelRestructureStep.create().withType(LabelRestructureStepType.MOVE).withPath(List.of("CATEGORY", "GONE")))))
			.exchange()
			.expectStatus().isBadRequest();

		verify(metadataServiceMock).restructureLabelTree(eq("MY_NAMESPACE"), eq("2281"), any());
	}

	@Test
	void restructureLabels_activeJob_returns409() {
		when(metadataServiceMock.startLabelTreeRestructure(eq("MY_NAMESPACE"), eq("2281"), any()))
			.thenThrow(Problem.valueOf(CONFLICT, "A job is already running"));

		webTestClient.post()
			.uri(builder -> builder.path(PATH + "/restructure").build(Map.of("namespace", "MY_NAMESPACE", "municipalityId", "2281")))
			.contentType(APPLICATION_JSON)
			.bodyValue(LabelRestructureRequest.create().withDryRun(false)
				.withSteps(List.of(LabelRestructureStep.create().withType(LabelRestructureStepType.ADD).withPath(List.of("CATEGORY")).withDisplayName("Category").withClassification("CATEGORY"))))
			.exchange()
			.expectStatus().isEqualTo(CONFLICT);

		verify(metadataServiceMock).startLabelTreeRestructure(eq("MY_NAMESPACE"), eq("2281"), any());
	}
}

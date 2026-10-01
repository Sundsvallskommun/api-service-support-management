package se.sundsvall.supportmanagement.api;

import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.supportmanagement.api.model.metadata.LabelClassification;
import se.sundsvall.supportmanagement.service.LabelClassificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ResourceTest
class MetadataLabelClassificationResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/label-classifications";

	@Autowired
	private LabelClassificationService labelClassificationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@ParameterizedTest
	@MethodSource("createArguments")
	void createWithInvalidArguments(final String namespace, final String municipalityId, final LabelClassification body, final Tuple expectedResponse) {
		final var response = webTestClient.post().uri(builder -> builder.path(PATH).build(Map.of("namespace", namespace, "municipalityId", municipalityId)))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactly(expectedResponse);

		verifyNoInteractions(labelClassificationServiceMock);
	}

	private static Stream<Arguments> createArguments() {
		final var valid = LabelClassification.create().withClassification("subtype");
		return Stream.of(
			Arguments.of("MY_NAMESPACE", "666", valid, tuple("createLabelClassification.municipalityId", "not a valid municipality ID")),
			Arguments.of("invalid,namespace", "2281", valid, tuple("createLabelClassification.namespace", "can only contain A-Z, a-z, 0-9, - and _")),
			Arguments.of("MY_NAMESPACE", "2281", LabelClassification.create().withDisplayName("Undertyp"), tuple("classification", "must not be blank")));
	}

	@Test
	void updateWithInvalidMunicipalityId() {
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{classification}").build(Map.of("namespace", "MY_NAMESPACE", "municipalityId", "666", "classification", "subtype")))
			.contentType(APPLICATION_JSON)
			.bodyValue(LabelClassification.create().withDisplayName("Undertyp"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message)
			.containsExactly(tuple("updateLabelClassification.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(labelClassificationServiceMock);
	}

	@Test
	void deleteWithInvalidNamespace() {
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{classification}").build(Map.of("namespace", "invalid,namespace", "municipalityId", "2281", "classification", "subtype")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message)
			.containsExactly(tuple("deleteLabelClassification.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(labelClassificationServiceMock);
	}
}

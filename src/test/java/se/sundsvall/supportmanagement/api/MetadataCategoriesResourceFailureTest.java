package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.stream.Stream;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
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
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.service.MetadataService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataCategoriesResourceFailureTest {

	@MockBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@ParameterizedTest
	@MethodSource("getWithInvalidParametersArguments")
	void getWithInvalidParameters(String url, Tuple expectedResponse) {
		final var response = webTestClient.get().uri(url)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(expectedResponse);

		verifyNoInteractions(metadataServiceMock);
	}

	private static Stream<Arguments> getWithInvalidParametersArguments() {
		return Stream.of(
			// Get category
			Arguments.of("2281/invalid,namespace/metadata/categories/category-name", tuple("getCategory.namespace", "can only contain A-Z, a-z, 0-9, -, _ and .")),
			Arguments.of("666/my.namespace/metadata/categories/category-name", tuple("getCategory.municipalityId", "not a valid municipality ID")),

			// Get categories
			Arguments.of("2281/invalid,namespace/metadata/categories", tuple("getCategories.namespace", "can only contain A-Z, a-z, 0-9, -, _ and .")),
			Arguments.of("666/my.namespace/metadata/categories", tuple("getCategories.municipalityId", "not a valid municipality ID")),

			// Get category type.
			Arguments.of("2281/invalid,namespace/metadata/categories/category-name/types", tuple("getCategoryTypes.namespace", "can only contain A-Z, a-z, 0-9, -, _ and .")),
			Arguments.of("666/my.namespace/metadata/categories/category-name/types", tuple("getCategoryTypes.municipalityId", "not a valid municipality ID")));
	}

	@Test
	void createWithInvalidNamespace() {

		final var response = webTestClient.post().uri("2281/invalid,namespace/metadata/categories")
			.bodyValue(ExternalIdType.create().withName("name"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("createCategory.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void createWithInvalidMunicipalityId() {

		final var response = webTestClient.post().uri("666/my.namespace/metadata/categories")
			.bodyValue(ExternalIdType.create().withName("name"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("createCategory.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void updateWithInvalidNamespace() {

		final var response = webTestClient.patch().uri("2281/invalid,namespace/metadata/categories/category-name")
			.bodyValue(Category.create().withName("name"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("updateCategory.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void updateWithInvalidMunicipalityId() {

		final var response = webTestClient.patch().uri("666/my.namespace/metadata/categories/category-name")
			.bodyValue(Category.create().withName("name"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("updateCategory.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void deleteWithInvalidNamespace() {

		final var response = webTestClient.delete().uri("2281/invalid,namespace/metadata/categories/category-name")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("deleteCategory.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void deleteWithInvalidMunicipalityId() {

		final var response = webTestClient.delete().uri("666/my.namespace/metadata/categories/category-name")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("deleteCategory.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(metadataServiceMock);
	}
}

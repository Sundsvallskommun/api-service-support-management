package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.zalando.problem.Status.BAD_REQUEST;

import org.junit.jupiter.api.Test;
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

	@Test
	void createWithInvalidNamespace() {

		final var response = webTestClient.post().uri("invalid,namespace/2281/metadata/categories")
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

		// TODO: Verify when service layer is ready
	}

	@Test
	void createWithInvalidMunicipalityId() {

		final var response = webTestClient.post().uri("my.namespace/666/metadata/categories")
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

		// TODO: Verify when service layer is ready
	}

	@Test
	void getCategoriesWithInvalidNamespace() {

		final var response = webTestClient.get().uri("invalid,namespace/2281/metadata/categories")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("getCategories.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// TODO: Verify when service layer is ready
	}

	@Test
	void getCategoriesWithInvalidMunicipalityId() {
		final var response = webTestClient.get().uri("my.namespace/666/metadata/categories")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("getCategories.municipalityId", "not a valid municipality ID"));

		// TODO: Verify when service layer is ready
	}

	@Test
	void getCategoryTypesWithInvalidNamespace() {

		final var response = webTestClient.get().uri("invalid,namespace/2281/metadata/categories/category-name/types")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("getCategoryTypes.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// TODO: Verify when service layer is ready
	}

	@Test
	void getCategoryTypesWithInvalidMunicipalityId() {
		final var response = webTestClient.get().uri("my.namespace/666/metadata/categories/category-name/types")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("getCategoryTypes.municipalityId", "not a valid municipality ID"));

		// TODO: Verify when service layer is ready
	}

	@Test
	void updateWithInvalidNamespace() {

		final var response = webTestClient.patch().uri("invalid,namespace/2281/metadata/categories/category-name")
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

		// TODO: Verify when service layer is ready
	}

	@Test
	void updateWithInvalidMunicipalityId() {

		final var response = webTestClient.patch().uri("my.namespace/666/metadata/categories/category-name")
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

		// TODO: Verify when service layer is ready
	}

	@Test
	void deleteWithInvalidNamespace() {

		final var response = webTestClient.delete().uri("invalid,namespace/2281/metadata/categories/category-name")
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

		// TODO: Verify when service layer is ready
	}

	@Test
	void deleteWithInvalidMunicipalityId() {

		final var response = webTestClient.delete().uri("my.namespace/666/metadata/categories/category-name")
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

		// TODO: Verify when service layer is ready
	}
}

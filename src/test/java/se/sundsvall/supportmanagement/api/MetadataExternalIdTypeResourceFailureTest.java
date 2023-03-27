package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.zalando.problem.Status.BAD_REQUEST;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.service.MetadataService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataExternalIdTypeResourceFailureTest {

	@MockBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@LocalServerPort
	private int port;

	@Test
	void createWithInvalidNamespace() {

		final var response = webTestClient.post().uri("invalid,namespace/2281/metadata/externalIdTypes")
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
			tuple("createExternalIdType.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// TODO: Verify when service layer is ready
	}

	@Test
	void createWithInvalidMunicipalityId() {

		final var response = webTestClient.post().uri("my.namespace/666/metadata/externalIdTypes")
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
			tuple("createExternalIdType.municipalityId", "not a valid municipality ID"));

		// TODO: Verify when service layer is ready
	}

	@Test
	void getWithInvalidNamespace() {

		final var response = webTestClient.get().uri("invalid,namespace/2281/metadata/externalIdTypes")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("getExternalIdTypes.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// TODO: Verify when service layer is ready
	}

	@Test
	void getWithInvalidMunicipalityId() {
		final var response = webTestClient.get().uri("my.namespace/666/metadata/externalIdTypes")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("getExternalIdTypes.municipalityId", "not a valid municipality ID"));

		// TODO: Verify when service layer is ready
	}

	@Test
	void deleteWithInvalidNamespace() {

		final var response = webTestClient.delete().uri("invalid,namespace/2281/metadata/externalIdTypes/external-id-type-name")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("deleteExternalIdType.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		// TODO: Verify when service layer is ready
	}

	@Test
	void deleteWithInvalidMunicipalityId() {

		final var response = webTestClient.delete().uri("my.namespace/666/metadata/externalIdTypes/external-id-type-name")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::getField, Violation::getMessage).containsExactlyInAnyOrder(
			tuple("deleteExternalIdType.municipalityId", "not a valid municipality ID"));

		// TODO: Verify when service layer is ready
	}
}

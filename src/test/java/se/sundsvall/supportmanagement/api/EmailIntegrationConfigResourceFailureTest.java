package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.config.EmailIntegration;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.service.MetadataService;
import se.sundsvall.supportmanagement.service.config.EmailIntegrationConfigService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class EmailIntegrationConfigResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/email-integration-config";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String INVALID = "#invalid#";

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private EmailIntegrationConfigService serviceMock;

	@MockBean
	private MetadataService metadataServiceMock;

	@BeforeEach
	void setup() {
		when(metadataServiceMock.findStatuses(any(), any())).thenReturn(List.of(Status.create().withName("NEW")));
	}

	@Test
	void createWithInvalidNamespace() {
		final var emailConfig = EmailIntegration.create().withEnabled(true).withStatusForNew("NEW");

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(emailConfig)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(
				tuple("createEmailIntegrationConfig.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithInvalidMunicipalityId() {
		final var emailConfig = EmailIntegration.create().withEnabled(true).withStatusForNew("NEW");

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(emailConfig)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("createEmailIntegrationConfig.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithStatusForNewNull() {
		final var emailConfig = EmailIntegration.create().withEnabled(true);

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(emailConfig)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("statusForNew", "must not be null"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithEnabledNull() {
		final var emailConfig = EmailIntegration.create().withStatusForNew("NEW");

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(emailConfig)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("enabled", "must not be null"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithInvalidStatus() {
		final var emailConfig = EmailIntegration.create().withEnabled(true).withStatusForNew("invalid");

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(emailConfig)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("statusForNew", "value 'invalid' doesn't match any of [NEW]"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readWithInvalidNamespace() {
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("readEmailIntegrationConfig.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readWithInvalidMunicipalityId() {
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("readEmailIntegrationConfig.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithInvalidNamespace() {
		final var emailConfig = EmailIntegration.create().withEnabled(true).withStatusForNew("NEW");

		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(emailConfig)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(
				tuple("updateEmailIntegrationConfig.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithInvalidMunicipalityId() {
		final var emailConfig = EmailIntegration.create().withEnabled(true).withStatusForNew("NEW");

		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(emailConfig)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("updateEmailIntegrationConfig.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithStatusForNewNull() {
		final var emailConfig = EmailIntegration.create().withEnabled(true);

		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(emailConfig)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("statusForNew", "must not be null"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithEnabledNull() {
		final var emailConfig = EmailIntegration.create().withStatusForNew("NEW");

		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(emailConfig)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("enabled", "must not be null"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithInvalidStatus() {
		final var emailConfig = EmailIntegration.create().withEnabled(true).withStatusForNew("invalid");

		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(emailConfig)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("statusForNew", "value 'invalid' doesn't match any of [NEW]"));

	}

	@Test
	void deleteWithInvalidNamespace() {
		final var response = webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("deleteEmailIntegrationConfig.namespace", "can only contain A-Z, a-z, 0-9, -, _ and ."));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteWithInvalidMunicipalityId() {
		final var response = webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("deleteEmailIntegrationConfig.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}
}

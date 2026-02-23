package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.api.model.config.action.Config;
import se.sundsvall.supportmanagement.api.model.config.action.Parameter;
import se.sundsvall.supportmanagement.service.ErrandActionService;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class NamespaceConfigResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/namespace-config";
	private static final String ACTION_CONFIG_PATH = "/{municipalityId}/{namespace}/namespace-config/action-config";
	private static final String ACTION_CONFIG_ID_PATH = "/{municipalityId}/{namespace}/namespace-config/action-config/{id}";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String DISPLAY_NAME = "DisplayName";
	private static final String SHORT_CODE = "NS";
	private static final String INVALID = "#invalid#";
	private static final String VALID_UUID = "a337d0de-5a6d-4952-9f39-74800b254c21";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private NamespaceConfigService serviceMock;

	@MockitoBean
	private ErrandActionService actionServiceMock;

	@Test
	void createWithInvalidNamespace() {
		final var namespaceConfig = createValidNamespaceConfig();

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
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
				tuple("createNamespaceConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithInvalidMunicipalityId() {
		final var namespaceConfig = createValidNamespaceConfig();

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
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
			.containsExactly(tuple("createNamespaceConfig.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithNamespaceNotNullInBody() {
		final var namespaceConfig = createValidNamespaceConfig()
			.withNamespace(NAMESPACE);

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
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
			.containsExactly(tuple("namespace", "must be null"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithMunicipalityIdNotNullInBody() {
		final var namespaceConfig = createValidNamespaceConfig()
			.withMunicipalityId(MUNICIPALITY_ID);

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
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
			.containsExactly(tuple("municipalityId", "must be null"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithDisplayNameNull() {
		final var namespaceConfig = NamespaceConfig.create().withShortCode(SHORT_CODE);

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
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
			.containsExactly(tuple("displayName", "must not be null"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithShortCodeNull() {
		final var namespaceConfig = NamespaceConfig.create().withDisplayName(DISPLAY_NAME);

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
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
			.containsExactly(tuple("shortCode", "must not be null"));

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
			.containsExactly(tuple("readNamespaceConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

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
			.containsExactly(tuple("readNamespaceConfig.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAllWithInvalidMunicipalityId() {
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/namespace-configs").queryParam("municipalityId", INVALID).build())
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
			.containsExactly(tuple("readAllNamespaceConfigs.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithInvalidNamespace() {
		final var namespaceConfig = createValidNamespaceConfig();

		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
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
				tuple("updateNamespaceConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithInvalidMunicipalityId() {
		final var namespaceConfig = createValidNamespaceConfig();

		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
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
			.containsExactly(tuple("updateNamespaceConfig.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithNamespaceNotNullInBody() {
		final var namespaceConfig = createValidNamespaceConfig()
			.withNamespace(NAMESPACE);

		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
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
			.containsExactly(tuple("namespace", "must be null"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithMunicipalityIdNotNullInBody() {
		final var namespaceConfig = createValidNamespaceConfig()
			.withMunicipalityId(MUNICIPALITY_ID);

		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
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
			.containsExactly(tuple("municipalityId", "must be null"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithDisplayNameNull() {
		final var namespaceConfig = NamespaceConfig.create().withShortCode(SHORT_CODE);

		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
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
			.containsExactly(tuple("displayName", "must not be null"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithShortCodeNull() {
		final var namespaceConfig = NamespaceConfig.create().withDisplayName(DISPLAY_NAME);

		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
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
			.containsExactly(tuple("shortCode", "must not be null"));

		verifyNoInteractions(serviceMock);
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
			.containsExactly(tuple("deleteNamespaceConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

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
			.containsExactly(tuple("deleteNamespaceConfig.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	// =============== Action Config ===============

	@Test
	void getActionConfigWithInvalidNamespace() {
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(ACTION_CONFIG_PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID)))
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
			.containsExactly(tuple("getActionConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(actionServiceMock);
	}

	@Test
	void getActionConfigWithInvalidMunicipalityId() {
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(ACTION_CONFIG_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID)))
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
			.containsExactly(tuple("getActionConfig.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(actionServiceMock);
	}

	@Test
	void createActionConfigWithInvalidNamespace() {
		final var config = createValidActionConfig();

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(ACTION_CONFIG_PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(config)
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
			.containsExactly(tuple("createActionConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(actionServiceMock);
	}

	@Test
	void createActionConfigWithInvalidMunicipalityId() {
		final var config = createValidActionConfig();

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(ACTION_CONFIG_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(config)
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
			.containsExactly(tuple("createActionConfig.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(actionServiceMock);
	}

	@Test
	void updateActionConfigWithInvalidNamespace() {
		final var config = createValidActionConfig();

		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(ACTION_CONFIG_ID_PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", VALID_UUID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(config)
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
			.containsExactly(tuple("updateActionConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(actionServiceMock);
	}

	@Test
	void updateActionConfigWithInvalidMunicipalityId() {
		final var config = createValidActionConfig();

		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(ACTION_CONFIG_ID_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", VALID_UUID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(config)
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
			.containsExactly(tuple("updateActionConfig.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(actionServiceMock);
	}

	@Test
	void updateActionConfigWithInvalidId() {
		final var config = createValidActionConfig();

		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(ACTION_CONFIG_ID_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(config)
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
			.containsExactly(tuple("updateActionConfig.id", "not a valid UUID"));

		verifyNoInteractions(actionServiceMock);
	}

	@Test
	void deleteActionConfigWithInvalidNamespace() {
		final var response = webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(ACTION_CONFIG_ID_PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "id", VALID_UUID)))
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
			.containsExactly(tuple("deleteActionConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(actionServiceMock);
	}

	@Test
	void deleteActionConfigWithInvalidMunicipalityId() {
		final var response = webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(ACTION_CONFIG_ID_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "id", VALID_UUID)))
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
			.containsExactly(tuple("deleteActionConfig.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(actionServiceMock);
	}

	@Test
	void deleteActionConfigWithInvalidId() {
		final var response = webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(ACTION_CONFIG_ID_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", INVALID)))
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
			.containsExactly(tuple("deleteActionConfig.id", "not a valid UUID"));

		verifyNoInteractions(actionServiceMock);
	}

	private static NamespaceConfig createValidNamespaceConfig() {
		return NamespaceConfig.create()
			.withDisplayName(DISPLAY_NAME)
			.withShortCode(SHORT_CODE);
	}

	private static Config createValidActionConfig() {
		return Config.create()
			.withName("ACTION_NAME")
			.withActive(true)
			.withConditions(List.of(Parameter.create().withKey("key").withValues(List.of("val"))))
			.withParameters(List.of(Parameter.create().withKey("key").withValues(List.of("val"))));
	}
}

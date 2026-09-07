package se.sundsvall.supportmanagement.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.api.model.config.Validation;
import se.sundsvall.supportmanagement.api.model.config.action.ActionDefinition;
import se.sundsvall.supportmanagement.api.model.config.action.Config;
import se.sundsvall.supportmanagement.api.model.config.action.Definition;
import se.sundsvall.supportmanagement.api.model.config.action.Parameter;
import se.sundsvall.supportmanagement.service.ErrandActionService;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.config.ValidationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EntityType.CATEGORY;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EntityType.STATUS;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class NamespaceConfigResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/namespace-config";
	private static final String ACTION_DEFINITION_PATH = "/{municipalityId}/{namespace}/namespace-config/action-definition";
	private static final String ACTION_CONFIG_PATH = "/{municipalityId}/{namespace}/namespace-config/action-config";
	private static final String ACTION_CONFIG_ID_PATH = "/{municipalityId}/{namespace}/namespace-config/action-config/{id}";
	private static final String VALIDATION_PATH = "/{municipalityId}/{namespace}/namespace-config/validation";
	private static final String VALIDATION_TYPE_PATH = "/{municipalityId}/{namespace}/namespace-config/validation/{type}";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String DISPLAY_NAME = "DisplayName";
	private static final String SHORT_CODE = "NS";
	private static final String CONFIG_ID = "a337d0de-5a6d-4952-9f39-74800b254c21";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private NamespaceConfigService serviceMock;

	@MockitoBean
	private ErrandActionService actionServiceMock;

	@MockitoBean
	private ValidationService validationServiceMock;

	@Test
	void create() {
		final var namespaceConfig = NamespaceConfig.create()
			.withDisplayName(DISPLAY_NAME)
			.withShortCode(SHORT_CODE);

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/namespace-config")
			.expectBody().isEmpty();

		verify(serviceMock).create(namespaceConfig, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void read() {
		final var namespaceConfig = NamespaceConfig.create()
			.withDisplayName(DISPLAY_NAME)
			.withShortCode(SHORT_CODE)
			.withCreated(OffsetDateTime.now().minusDays(2))
			.withModified(OffsetDateTime.now().minusDays(1));

		when(serviceMock.get(any(), any())).thenReturn(namespaceConfig);

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(NamespaceConfig.class)
			.returnResult()
			.getResponseBody();

		verify(serviceMock).get(NAMESPACE, MUNICIPALITY_ID);
		assertThat(response).isNotNull().isEqualTo(namespaceConfig);
	}

	@Test
	void readAll() {
		final var configs = List.of(NamespaceConfig.create()
			.withDisplayName(DISPLAY_NAME)
			.withShortCode(SHORT_CODE)
			.withCreated(OffsetDateTime.now().minusDays(2))
			.withModified(OffsetDateTime.now().minusDays(1)));

		when(serviceMock.findAll(any())).thenReturn(configs);

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/namespace-configs").build())
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(NamespaceConfig.class)
			.returnResult()
			.getResponseBody();

		verify(serviceMock).findAll(null);
		assertThat(response).isNotNull().isEqualTo(configs);
	}

	@Test
	void readAllWithMunicipalityId() {
		final var configs = List.of(NamespaceConfig.create()
			.withDisplayName(DISPLAY_NAME)
			.withShortCode(SHORT_CODE)
			.withCreated(OffsetDateTime.now().minusDays(2))
			.withModified(OffsetDateTime.now().minusDays(1)));

		when(serviceMock.findAll(any())).thenReturn(configs);

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/namespace-configs").queryParam("municipalityId", MUNICIPALITY_ID).build())
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(NamespaceConfig.class)
			.returnResult()
			.getResponseBody();

		verify(serviceMock).findAll(MUNICIPALITY_ID);
		assertThat(response).isNotNull().isEqualTo(configs);
	}

	@Test
	void update() {
		final var namespaceConfig = NamespaceConfig.create()
			.withDisplayName(DISPLAY_NAME)
			.withShortCode(SHORT_CODE);

		webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(serviceMock).replace(namespaceConfig, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void delete() {
		webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(serviceMock).delete(NAMESPACE, MUNICIPALITY_ID);
	}

	// =============== Action ===============

	@Test
	void getActionDefinitions() {
		final var definitions = List.of(ActionDefinition.create()
			.withName("ADD_LABEL")
			.withDescription("Adds a label")
			.withConditionDefinitions(List.of(Definition.create().withKey("status").withMandatory(true)))
			.withParameterDefinitions(List.of(Definition.create().withKey("label").withMandatory(true))));

		when(actionServiceMock.getActionDefinitions(any(), any())).thenReturn(definitions);

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(ACTION_DEFINITION_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(ActionDefinition.class)
			.returnResult()
			.getResponseBody();

		verify(actionServiceMock).getActionDefinitions(MUNICIPALITY_ID, NAMESPACE);
		assertThat(response).isNotNull().isEqualTo(definitions);
	}

	// =============== Action Config ===============

	@Test
	void getActionConfigs() {
		final var configs = List.of(Config.create()
			.withId(CONFIG_ID)
			.withName("ACTION_NAME")
			.withActive(true)
			.withDisplayValue("Display")
			.withConditions(List.of(Parameter.create().withKey("key").withValues(List.of("val"))))
			.withParameters(List.of(Parameter.create().withKey("key").withValues(List.of("val")))));

		when(actionServiceMock.getActionConfigs(any(), any())).thenReturn(configs);

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(ACTION_CONFIG_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Config.class)
			.returnResult()
			.getResponseBody();

		verify(actionServiceMock).getActionConfigs(MUNICIPALITY_ID, NAMESPACE);
		assertThat(response).isNotNull().isEqualTo(configs);
	}

	@Test
	void createActionConfig() {
		final var config = Config.create()
			.withName("ACTION_NAME")
			.withActive(true)
			.withConditions(List.of(Parameter.create().withKey("key").withValues(List.of("val"))))
			.withParameters(List.of(Parameter.create().withKey("key").withValues(List.of("val"))));

		when(actionServiceMock.createActionConfig(any(), any(), any())).thenReturn(CONFIG_ID);

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(ACTION_CONFIG_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(config)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/namespace-config/action-config/" + CONFIG_ID)
			.expectBody().isEmpty();

		verify(actionServiceMock).createActionConfig(MUNICIPALITY_ID, NAMESPACE, config);
	}

	@Test
	void updateActionConfig() {
		final var config = Config.create()
			.withName("ACTION_NAME")
			.withActive(true)
			.withConditions(List.of(Parameter.create().withKey("key").withValues(List.of("val"))))
			.withParameters(List.of(Parameter.create().withKey("key").withValues(List.of("val"))));

		webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(ACTION_CONFIG_ID_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", CONFIG_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(config)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(actionServiceMock).updateActionConfig(MUNICIPALITY_ID, NAMESPACE, CONFIG_ID, config);
	}

	// =============== Validation ===============

	@Test
	void getValidations() {
		final var validations = List.of(
			Validation.create().withType(STATUS).withValidated(true),
			Validation.create().withType(CATEGORY).withValidated(false));

		when(validationServiceMock.findAll(any(), any())).thenReturn(validations);

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(VALIDATION_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Validation.class)
			.returnResult()
			.getResponseBody();

		verify(validationServiceMock).findAll(NAMESPACE, MUNICIPALITY_ID);
		assertThat(response).isNotNull().isEqualTo(validations);
	}

	@Test
	void updateValidation() {
		final var request = Validation.create().withValidated(true);
		final var updated = Validation.create().withType(STATUS).withValidated(true);

		when(validationServiceMock.update(any(), any(), any(), any())).thenReturn(updated);

		final var response = webTestClient.patch()
			.uri(uriBuilder -> uriBuilder.path(VALIDATION_TYPE_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "type", STATUS)))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Validation.class)
			.returnResult()
			.getResponseBody();

		verify(validationServiceMock).update(NAMESPACE, MUNICIPALITY_ID, STATUS, request);
		assertThat(response).isNotNull().isEqualTo(updated);
	}

	@Test
	void deleteActionConfig() {
		webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(ACTION_CONFIG_ID_PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "id", CONFIG_ID)))
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(actionServiceMock).deleteActionConfig(MUNICIPALITY_ID, NAMESPACE, CONFIG_ID);
	}
}

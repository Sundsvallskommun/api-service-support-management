package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.config.MessageExchangeIntegration;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.service.MetadataService;
import se.sundsvall.supportmanagement.service.config.MessageExchangeIntegrationConfigService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MessageExchangeIntegrationConfigResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/messageexchange-integration-config";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String INVALID = "#invalid#";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private MessageExchangeIntegrationConfigService serviceMock;

	@MockitoBean
	private MetadataService metadataServiceMock;

	@BeforeEach
	void setup() {
		when(metadataServiceMock.findStatuses(any(), any(), any(Sort.class)))
			.thenReturn(List.of(Status.create().withName("SOLVED"), Status.create().withName("OPEN")));
	}

	@Test
	void createWithInvalidNamespace() {
		final var config = MessageExchangeIntegration.create().withTriggerStatusChangeOn("SOLVED").withStatusChangeTo("OPEN");

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID)))
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
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("createMessageExchangeIntegrationConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithInvalidMunicipalityId() {
		final var config = MessageExchangeIntegration.create().withTriggerStatusChangeOn("SOLVED").withStatusChangeTo("OPEN");

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID)))
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
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("createMessageExchangeIntegrationConfig.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithInvalidTriggerStatus() {
		final var config = MessageExchangeIntegration.create().withTriggerStatusChangeOn("invalid").withStatusChangeTo("OPEN");

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
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
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("triggerStatusChangeOn", "value 'invalid' doesn't match any of [SOLVED, OPEN]"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithInvalidStatusChangeTo() {
		final var config = MessageExchangeIntegration.create().withTriggerStatusChangeOn("SOLVED").withStatusChangeTo("invalid");

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
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
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("statusChangeTo", "value 'invalid' doesn't match any of [SOLVED, OPEN]"));

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
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("readMessageExchangeIntegrationConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

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
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("readMessageExchangeIntegrationConfig.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithInvalidNamespace() {
		final var config = MessageExchangeIntegration.create().withTriggerStatusChangeOn("SOLVED").withStatusChangeTo("OPEN");

		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID)))
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
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("updateMessageExchangeIntegrationConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

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
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("deleteMessageExchangeIntegrationConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(serviceMock);
	}
}

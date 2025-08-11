package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.config.MessageExchangeSync;
import se.sundsvall.supportmanagement.service.config.MessageExchangeSyncConfigService;

@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class MessageExchangeSyncConfigResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final Long CONFIG_ID = 1L;
	private static final String INVALID_ID = "#invalid#";

	private static final String BASE_PATH = "/{municipalityId}/message-exchange-sync-config";
	private static final String PATH_WITH_ID = BASE_PATH + "/{id}";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private MessageExchangeSyncConfigService serviceMock;

	@Test
	void createWithInvalidMunicipalityId() {
		final var syncConfig = MessageExchangeSync.create().withActive(true);

		final var response = webTestClient.post()
			.uri(BASE_PATH, INVALID_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(syncConfig)
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
			.containsExactly(tuple("createMessageExchangeSyncConfig.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void getAllByMunicipalityIdWithInvalidId() {
		final var response = webTestClient.get()
			.uri(BASE_PATH, INVALID_ID)
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
			.containsExactly(tuple("getAllMessageExchangeByMunicipalityId.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithNullBody() {
		final var response = webTestClient.post()
			.uri(BASE_PATH, MUNICIPALITY_ID)
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Bad Request");
		assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST);
		assertThat(response.getDetail()).contains("request body is missing");

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithInvalidBodyField() {
		final var invalidSyncConfig = MessageExchangeSync.create();

		final var response = webTestClient.post()
			.uri(BASE_PATH, MUNICIPALITY_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(invalidSyncConfig)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("active", "must not be null"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithInvalidBodyField() {
		final var invalidSyncConfig = MessageExchangeSync.create();

		final var response = webTestClient.put()
			.uri(PATH_WITH_ID, MUNICIPALITY_ID, CONFIG_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(invalidSyncConfig)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactly(tuple("active", "must not be null"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWhenNotFound() {
		final var syncConfig = MessageExchangeSync.create().withActive(true);
		doThrow(Problem.valueOf(NOT_FOUND, "Config not found"))
			.when(serviceMock).replace(any(MessageExchangeSync.class), eq(MUNICIPALITY_ID), eq(CONFIG_ID));

		final var response = webTestClient.put()
			.uri(PATH_WITH_ID, MUNICIPALITY_ID, CONFIG_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(syncConfig)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Not Found");
		assertThat(response.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(response.getDetail()).isEqualTo("Config not found");
	}

	@Test
	void deleteWhenNotFound() {
		doThrow(Problem.valueOf(NOT_FOUND, "Config not found"))
			.when(serviceMock).delete(MUNICIPALITY_ID, CONFIG_ID);

		final var response = webTestClient.delete()
			.uri(PATH_WITH_ID, MUNICIPALITY_ID, CONFIG_ID)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Not Found");
		assertThat(response.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(response.getDetail()).isEqualTo("Config not found");
	}
}

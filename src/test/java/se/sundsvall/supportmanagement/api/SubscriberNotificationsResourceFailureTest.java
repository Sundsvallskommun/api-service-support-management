package se.sundsvall.supportmanagement.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.SubscriberNotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class SubscriberNotificationsResourceFailureTest {

	private static final String BASE_PATH = "/{municipalityId}/{namespace}/notifications";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NOTIFICATION_ID = "74540a24-70e1-4e82-90f7-7d8ad4666cdc";
	private static final String IDENTIFIER_TYPE = "adAccount";
	private static final String IDENTIFIER_VALUE = "joe01doe";
	private static final String INVALID = "#invalid#";

	@MockitoBean
	private SubscriberNotificationService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getNotificationsWithInvalidMunicipalityId() {
		final var response = webTestClient.get()
			.uri(builder -> builder.path(BASE_PATH + "/{identifierType}/{identifierValue}")
				.build(Map.of("municipalityId", INVALID, "namespace", NAMESPACE, "identifierType", IDENTIFIER_TYPE, "identifierValue", IDENTIFIER_VALUE)))
			.accept(APPLICATION_JSON)
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
			.containsExactly(tuple("getNotifications.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void getNotificationsWithInvalidNamespace() {
		final var response = webTestClient.get()
			.uri(builder -> builder.path(BASE_PATH + "/{identifierType}/{identifierValue}")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", INVALID, "identifierType", IDENTIFIER_TYPE, "identifierValue", IDENTIFIER_VALUE)))
			.accept(APPLICATION_JSON)
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
			.containsExactly(tuple("getNotifications.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteNotificationWithInvalidMunicipalityId() {
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(BASE_PATH + "/{notificationId}")
				.build(Map.of("municipalityId", INVALID, "namespace", NAMESPACE, "notificationId", NOTIFICATION_ID)))
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
			.containsExactly(tuple("deleteNotification.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteNotificationWithInvalidNamespace() {
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(BASE_PATH + "/{notificationId}")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", INVALID, "notificationId", NOTIFICATION_ID)))
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
			.containsExactly(tuple("deleteNotification.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteNotificationWithInvalidNotificationId() {
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(BASE_PATH + "/{notificationId}")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "notificationId", INVALID)))
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
			.containsExactly(tuple("deleteNotification.notificationId", "not a valid UUID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void acknowledgeNotificationWithInvalidMunicipalityId() {
		final var response = webTestClient.put()
			.uri(builder -> builder.path(BASE_PATH + "/{notificationId}/acknowledge")
				.build(Map.of("municipalityId", INVALID, "namespace", NAMESPACE, "notificationId", NOTIFICATION_ID)))
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
			.containsExactly(tuple("acknowledgeNotification.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void acknowledgeNotificationWithInvalidNamespace() {
		final var response = webTestClient.put()
			.uri(builder -> builder.path(BASE_PATH + "/{notificationId}/acknowledge")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", INVALID, "notificationId", NOTIFICATION_ID)))
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
			.containsExactly(tuple("acknowledgeNotification.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void acknowledgeNotificationWithInvalidNotificationId() {
		final var response = webTestClient.put()
			.uri(builder -> builder.path(BASE_PATH + "/{notificationId}/acknowledge")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "notificationId", INVALID)))
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
			.containsExactly(tuple("acknowledgeNotification.notificationId", "not a valid UUID"));

		verifyNoInteractions(serviceMock);
	}
}

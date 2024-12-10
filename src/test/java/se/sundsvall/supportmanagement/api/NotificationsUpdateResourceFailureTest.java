package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
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
import se.sundsvall.supportmanagement.TestObjectsBuilder;
import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.service.NotificationService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class NotificationsUpdateResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/notifications";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String INVALID = "can only contain A-Z, a-z, 0-9, -, _ and .";

	@MockBean
	private NotificationService notificationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static Stream<Arguments> provideBadRequests() {
		return Stream.of(
			Arguments.of(List.of(TestObjectsBuilder.createNotification(n -> n.withOwnerFullName(null))), "updateNotifications.notifications[0].ownerFullName", "must not be blank"),
			Arguments.of(List.of(TestObjectsBuilder.createNotification(n -> n.withOwnerId(null))), "updateNotifications.notifications[0].ownerId", "must not be blank"),
			Arguments.of(List.of(TestObjectsBuilder.createNotification(n -> n.withType(null))), "updateNotifications.notifications[0].type", "must not be blank"),
			Arguments.of(List.of(TestObjectsBuilder.createNotification(n -> n.withDescription(null))), "updateNotifications.notifications[0].description", "must not be blank"),
			Arguments.arguments(List.of(), "updateNotifications.notifications", "must not be empty"));
	}

	@ParameterizedTest
	@MethodSource("provideBadRequests")
	void updateNotificationsWithInvalidInputs(final List<Notification> notifications, final String expectedField, final String expectedMessage) {

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.bodyValue(notifications)
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
			.containsExactly(tuple(expectedField, expectedMessage));

		// Verification
		verifyNoInteractions(notificationServiceMock);
	}

	@Test
	void updateNotificationsWithInvalidNamespace() {
		// Parameter values
		final var requestBody = List.of(TestObjectsBuilder.createNotification(n -> {}));

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		verifyNoInteractions(notificationServiceMock);
	}

	@Test
	void updateNotificationsWithInvalidMunicipalityId() {
		// Parameter values
		final var requestBody = List.of(TestObjectsBuilder.createNotification(n -> {}));

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		verifyNoInteractions(notificationServiceMock);
	}
}

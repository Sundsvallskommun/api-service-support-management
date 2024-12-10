package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.problem.Status.NOT_FOUND;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.NotificationService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class NotificationsReadResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/notifications";
	private static final String PATH_NOTIFICATION = PATH + "/{notificationId}";
	private static final String PATH_NOTIFICATIONS = PATH + "?ownerId={ownerId}";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String OWNER_ID = UUID.randomUUID().toString();
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String NOTIFICATION_ID = UUID.randomUUID().toString();
	private static final String INVALID = "#invalid#";

	@MockBean
	private NotificationService notificationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getNotificationsWithInvalidNamespace() {
		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_NOTIFICATIONS).build(INVALID, MUNICIPALITY_ID, ERRAND_ID, OWNER_ID))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		verifyNoInteractions(notificationServiceMock);
	}

	@Test
	void getNotificationsWithInvalidMunicipalityId() {
		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_NOTIFICATIONS).build(NAMESPACE, INVALID, ERRAND_ID, OWNER_ID))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		verifyNoInteractions(notificationServiceMock);
	}

	@Test
	void getNotificationsWithInvalidOwnerId() {
		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_NOTIFICATIONS).build(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVALID))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		verifyNoInteractions(notificationServiceMock);
	}

	@Test
	void getNotificationWithInvalidNamespace() {
		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_NOTIFICATION).build(INVALID, MUNICIPALITY_ID, ERRAND_ID, NOTIFICATION_ID))
			.accept(APPLICATION_JSON)
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
	void getNotificationWithInvalidMunicipalityId() {
		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_NOTIFICATION).build(NAMESPACE, INVALID, ERRAND_ID, NOTIFICATION_ID))
			.accept(APPLICATION_JSON)
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
	void getNotificationNotFound() {

		// Arrange
		when(notificationServiceMock.getNotification(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTIFICATION_ID)).thenThrow(Problem.valueOf(NOT_FOUND, String.format("Notification with id %s not found in namespace %s for municipality with id %s",
			NOTIFICATION_ID,
			NAMESPACE, MUNICIPALITY_ID)));

		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH_NOTIFICATION).build(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTIFICATION_ID))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		assertThat(response.getDetail()).isEqualTo(String.format("Notification with id %s not found in namespace %s for municipality with id %s", NOTIFICATION_ID, NAMESPACE, MUNICIPALITY_ID));
	}
}

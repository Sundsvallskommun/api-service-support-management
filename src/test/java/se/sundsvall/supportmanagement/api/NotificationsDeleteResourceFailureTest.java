package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.zalando.problem.Status.NOT_FOUND;

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
class NotificationsDeleteResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/notifications/{notificationId}";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NOTIFICATION_ID = "123e4567-e89b-12d3-a456-426614174000";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String INVALID = "#invalid#";

	@MockBean
	private NotificationService notificationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void deleteNotificationWithInvalidNamespace() {
		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH).build(INVALID, MUNICIPALITY_ID, ERRAND_ID, NOTIFICATION_ID))
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
	void deleteNotificationWithInvalidMunicipalityId() {
		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH).build(NAMESPACE, INVALID, ERRAND_ID, NOTIFICATION_ID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		assertThat(response.getViolations()).isNotEmpty();

		verifyNoInteractions(notificationServiceMock);
	}

	@Test
	void deleteNotificationWithInvalidErrandId() {

		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH).build(MUNICIPALITY_ID, NAMESPACE, INVALID, NOTIFICATION_ID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		assertThat(response.getViolations()).isNotEmpty();

		verifyNoInteractions(notificationServiceMock);
	}

	@Test
	void deleteNotificationWithInvalidNotificationId() {

		doThrow(Problem.valueOf(NOT_FOUND, "Notification id not found")).when(notificationServiceMock).deleteNotification(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, INVALID);

		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(PATH).build(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, INVALID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();
		assertThat(response.getViolations()).isNotEmpty();
		assertThat(response.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(response.getDetail()).isEqualTo("Notification id not found");
	}
}

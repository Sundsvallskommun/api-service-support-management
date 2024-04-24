package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.CONFLICT;

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
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.TestObjectsBuilder;
import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.service.NotificationService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class NotificationsCreateResourceFailureTest {

	private static final String PATH = "/{namespace}/{municipalityId}/notifications";

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String INVALID = "can only contain A-Z, a-z, 0-9, -, _ and .";

	@MockBean
	private NotificationService notificationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static Stream<Arguments> provideBadRequests() {
		return Stream.of(
			Arguments.of(TestObjectsBuilder.createNotification(n -> n.withOwnerFullName(null)), "ownerFullName", "must not be blank"),
			Arguments.of(TestObjectsBuilder.createNotification(n -> n.withOwnerId(null)), "ownerId", "must not be blank"),
			Arguments.of(TestObjectsBuilder.createNotification(n -> n.withCreatedBy(null)), "createdBy", "must not be blank"),
			Arguments.of(TestObjectsBuilder.createNotification(n -> n.withType(null)), "type", "must not be blank"),
			Arguments.of(TestObjectsBuilder.createNotification(n -> n.withDescription(null)), "description", "must not be blank"),
			Arguments.of(TestObjectsBuilder.createNotification(n -> n.withErrandId(null)), "errandId", "not a valid UUID")
		);
	}

	@ParameterizedTest
	@MethodSource("provideBadRequests")
	void createNotificationWithInvalidInputs(final Notification notification, final String expectedField, final String expectedMessage) {

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.bodyValue(notification)
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
	void createNotificationWithInvalidNamespace() {
		// Parameter values
		final var requestBody = TestObjectsBuilder.createNotification(n -> {});

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID)))
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
	void createNotificationWithInvalidMunicipalityId() {
		// Parameter values
		final var requestBody = TestObjectsBuilder.createNotification(n -> {});

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID)))
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
	void createNotificationAlreadyExists() {
		// Parameter values
		final var requestBody = TestObjectsBuilder.createNotification(n -> {});
		when(notificationServiceMock.createNotification(MUNICIPALITY_ID, NAMESPACE, requestBody)).thenReturn(null);

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().is4xxClientError()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		// Verification
		assertThat(response).isNotNull();

		assertThat(response.getStatus()).isEqualTo(CONFLICT);
		assertThat(response.getTitle()).isEqualTo("Conflict");
		assertThat(response.getDetail()).isEqualTo("Notification already exists");
	}

}

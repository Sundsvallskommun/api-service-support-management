package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.violations.ConstraintViolationProblem;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.NotificationService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class NotificationsReadResourceFailureTest {

	private static final String PATH = "/{namespace}/{municipalityId}/notifications?ownerId={ownerId}";

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String OWNER_ID = UUID.randomUUID().toString();

	private static final String INVALID = "#invalid#";

	@MockBean
	private NotificationService notificationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getNotificationsWithInvalidNamespace() {
		// Call
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(INVALID, MUNICIPALITY_ID, OWNER_ID))
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
			.uri(builder -> builder.path(PATH).build(NAMESPACE, INVALID, OWNER_ID))
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
			.uri(builder -> builder.path(PATH).build(NAMESPACE, MUNICIPALITY_ID, INVALID))
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

}

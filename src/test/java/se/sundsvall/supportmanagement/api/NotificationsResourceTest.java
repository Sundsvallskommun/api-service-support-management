package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.service.NotificationService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class NotificationsResourceTest {

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	@MockBean
	private NotificationService notificationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getNotifications() {
		// Mock
		when(notificationServiceMock.getNotifications(MUNICIPALITY_ID, NAMESPACE, "12")).thenReturn(List.of(Notification.create()));

		final var response = webTestClient.get()
			.uri("/" + NAMESPACE + "/" + MUNICIPALITY_ID + "/notifications" + "?ownerId=12")
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(List.class)
			.returnResult();

		// Verification
		assertThat(response).isNotNull();
		verify(notificationServiceMock).getNotifications(MUNICIPALITY_ID, NAMESPACE, "12");
	}

	@Test
	void createNotification() {
		// Parameter values
		final var requestBody = Notification.create()
			.withOwner("SomeOwner")
			.withOwnerId(UUID.randomUUID().toString())
			.withCreatedBy("SomeUser")
			.withType("SomeType")
			.withDescription("Some description")
			.withErrandId(UUID.randomUUID().toString())
			.withAcknowledged(true);

		// Mock
		final var notificationId = UUID.randomUUID().toString();
		when(notificationServiceMock.createNotification(MUNICIPALITY_ID, NAMESPACE, requestBody)).thenReturn(notificationId);

		// Call
		final var response = webTestClient.post()
			.uri("/" + NAMESPACE + "/" + MUNICIPALITY_ID + "/notifications")
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + NAMESPACE + "/" + MUNICIPALITY_ID + "/notifications/" + notificationId)
			.expectBody().isEmpty();

		// Verification
		assertThat(response).isNotNull();
		verify(notificationServiceMock).createNotification(MUNICIPALITY_ID, NAMESPACE, requestBody);
	}

	@Test
	void updateNotification() {
		// Parameter values
		final var notificationId = UUID.randomUUID().toString();
		final var requestBody = Notification.create()
			.withId(notificationId)
			.withOwner("TestOwner")
			.withOwnerId("12")
			.withCreatedBy("TestUser")
			.withType("CREATE")
			.withDescription("Test description")
			.withContent("Test content")
			.withAcknowledged(true);

		// Call
		final var response = webTestClient.patch()
			.uri("/" + NAMESPACE + "/" + MUNICIPALITY_ID + "/notifications/" + notificationId)
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		// Verification
		assertThat(response).isNotNull();
		verify(notificationServiceMock).updateNotification(MUNICIPALITY_ID, NAMESPACE, notificationId, requestBody);
	}

	@Test
	void deleteNotification() {
		// Parameter values
		final var notificationId = UUID.randomUUID().toString();

		// Call
		final var response = webTestClient.delete()
			.uri("/" + NAMESPACE + "/" + MUNICIPALITY_ID + "/notifications/" + notificationId)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		// Verification
		assertThat(response).isNotNull();
		verify(notificationServiceMock).deleteNotification(MUNICIPALITY_ID, NAMESPACE, notificationId);
	}

}

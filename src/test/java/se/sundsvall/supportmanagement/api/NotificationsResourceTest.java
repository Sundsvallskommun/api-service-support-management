package se.sundsvall.supportmanagement.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.TestObjectsBuilder;
import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.service.NotificationService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class NotificationsResourceTest {

	private static final String GLOBAL_NOTIFICATION_PATH = "/{municipalityId}/{namespace}/notifications";
	private static final String ERRAND_NOTIFICATION_PATH = "/{municipalityId}/{namespace}/errands/{errandId}/notifications";
	private static final String NAMESPACE = "namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String MUNICIPALITY_ID = "2281";

	@MockitoBean
	private NotificationService notificationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getNotificationsByOwnerId() {
		// Mock
		when(notificationServiceMock.getNotificationsByOwnerId(MUNICIPALITY_ID, NAMESPACE, "12")).thenReturn(List.of(Notification.create()));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(GLOBAL_NOTIFICATION_PATH)
				.queryParam("ownerId", "12")
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(List.class)
			.returnResult();

		// Verification
		assertThat(response).isNotNull();
		verify(notificationServiceMock).getNotificationsByOwnerId(MUNICIPALITY_ID, NAMESPACE, "12");
	}

	@Test
	void createNotification() {
		// Parameter values
		final var requestBody = Notification.create()
			.withOwnerId("SomeOwnerId")
			.withCreatedBy("SomeUser")
			.withType("SomeType")
			.withSubtype("SomeSubtype")
			.withDescription("Some description")
			.withAcknowledged(true);

		// Mock
		final var notificationId = randomUUID().toString();
		when(notificationServiceMock.createNotification(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, requestBody)).thenReturn(notificationId);

		// Call
		final var response = webTestClient.post()
			.uri(builder -> builder.path(ERRAND_NOTIFICATION_PATH)
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/notifications/" + notificationId)
			.expectBody().isEmpty();

		// Verification
		assertThat(response).isNotNull();
		verify(notificationServiceMock).createNotification(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, requestBody);
	}

	@Test
	void globalAcknowledgeNotifications() {

		// Mock
		doNothing().when(notificationServiceMock).globalAcknowledgeNotificationsByErrandId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		// Call
		final var response = webTestClient.put()
			.uri(builder -> builder.path(ERRAND_NOTIFICATION_PATH + "/global-acknowledged")
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().contentType(ALL)
			.expectBody().isEmpty();

		// Verification
		assertThat(response).isNotNull();
		verify(notificationServiceMock).globalAcknowledgeNotificationsByErrandId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void updateNotification() {
		// Parameter values
		final var notificationId = randomUUID().toString();
		final var requestBody = List.of(TestObjectsBuilder.createNotification(n -> n.withId(notificationId)));

		// Call
		final var response = webTestClient.patch()
			.uri(builder -> builder.path(GLOBAL_NOTIFICATION_PATH)
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		// Verification
		assertThat(response).isNotNull();
		verify(notificationServiceMock).updateNotifications(MUNICIPALITY_ID, NAMESPACE, requestBody);
	}

	@Test
	void deleteNotification() {
		// Parameter values
		final var notificationId = randomUUID().toString();

		// Call
		final var response = webTestClient.delete()
			.uri(builder -> builder.path(ERRAND_NOTIFICATION_PATH + "/" + notificationId)
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		// Verification
		assertThat(response).isNotNull();
		verify(notificationServiceMock).deleteNotification(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, notificationId);
	}
}

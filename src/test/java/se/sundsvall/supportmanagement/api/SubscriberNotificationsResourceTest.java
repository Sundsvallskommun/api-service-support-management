package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.notification.SubscriberNotification;
import se.sundsvall.supportmanagement.service.SubscriberNotificationService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class SubscriberNotificationsResourceTest {

	private static final String BASE_PATH = "/{municipalityId}/{namespace}/notifications";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String NOTIFICATION_ID = randomUUID().toString();
	private static final String IDENTIFIER_TYPE = "adAccount";
	private static final String IDENTIFIER_VALUE = "joe01doe";

	@MockitoBean
	private SubscriberNotificationService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getNotifications() {
		final var notification = SubscriberNotification.create()
			.withId(NOTIFICATION_ID)
			.withIdentifierType(IDENTIFIER_TYPE)
			.withIdentifierValue(IDENTIFIER_VALUE);

		when(serviceMock.getNotifications(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER_TYPE, IDENTIFIER_VALUE))
			.thenReturn(List.of(notification));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(BASE_PATH + "/{identifierType}/{identifierValue}")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE,
					"identifierType", IDENTIFIER_TYPE, "identifierValue", IDENTIFIER_VALUE)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(new ParameterizedTypeReference<List<SubscriberNotification>>() {})
			.returnResult();

		assertThat(response.getResponseBody()).hasSize(1);
		assertThat(response.getResponseBody().getFirst().getId()).isEqualTo(NOTIFICATION_ID);
		verify(serviceMock).getNotifications(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER_TYPE, IDENTIFIER_VALUE);
	}

	@Test
	void deleteNotification() {
		doNothing().when(serviceMock).deleteNotification(MUNICIPALITY_ID, NAMESPACE, NOTIFICATION_ID);

		webTestClient.delete()
			.uri(builder -> builder.path(BASE_PATH + "/{notificationId}")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "notificationId", NOTIFICATION_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).deleteNotification(MUNICIPALITY_ID, NAMESPACE, NOTIFICATION_ID);
	}

	@Test
	void acknowledgeNotification() {
		doNothing().when(serviceMock).acknowledgeNotification(MUNICIPALITY_ID, NAMESPACE, NOTIFICATION_ID);

		webTestClient.put()
			.uri(builder -> builder.path(BASE_PATH + "/{notificationId}/acknowledge")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "notificationId", NOTIFICATION_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).acknowledgeNotification(MUNICIPALITY_ID, NAMESPACE, NOTIFICATION_ID);
	}

	@Test
	void getNotifications_invalidMunicipalityId() {
		webTestClient.get()
			.uri(builder -> builder.path(BASE_PATH + "/{identifierType}/{identifierValue}")
				.build(Map.of("municipalityId", "not-valid", "namespace", NAMESPACE,
					"identifierType", IDENTIFIER_TYPE, "identifierValue", IDENTIFIER_VALUE)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest();
	}

	@Test
	void deleteNotification_invalidNotificationId() {
		webTestClient.delete()
			.uri(builder -> builder.path(BASE_PATH + "/{notificationId}")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "notificationId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();
	}

	@Test
	void acknowledgeNotification_invalidNotificationId() {
		webTestClient.put()
			.uri(builder -> builder.path(BASE_PATH + "/{notificationId}/acknowledge")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "notificationId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();
	}
}

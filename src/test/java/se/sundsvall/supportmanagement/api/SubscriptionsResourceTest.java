package se.sundsvall.supportmanagement.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.subscription.Subscription;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTarget;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTargetType;
import se.sundsvall.supportmanagement.service.SubscriptionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class SubscriptionsResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/subscribers/{subscriberId}/subscriptions";
	private static final String NAMESPACE = "my-namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String SUBSCRIBER_ID = "123e4567-e89b-12d3-a456-426614174000";
	private static final String SUBSCRIPTION_ID = "f0882f1d-06bc-47fd-b017-1d8307f5ce95";
	private static final String ERRAND_ID = "b82bd8ac-1507-4d9a-958d-369261eecc15";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private SubscriptionService serviceMock;

	@Test
	void getSubscriptions() {
		final var subscription = Subscription.create()
			.withId(SUBSCRIPTION_ID)
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND).withId(ERRAND_ID))
			.withCreated(OffsetDateTime.now());

		when(serviceMock.findSubscriptions(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(List.of(subscription));

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Subscription.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).containsExactly(subscription);
		verify(serviceMock).findSubscriptions(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void createSubscription() {
		final var subscription = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND).withId(ERRAND_ID));

		when(serviceMock.createSubscription(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SUBSCRIBER_ID), any(Subscription.class)))
			.thenReturn(SUBSCRIPTION_ID);

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(subscription)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/subscribers/" + SUBSCRIBER_ID + "/subscriptions/" + SUBSCRIPTION_ID)
			.expectBody().isEmpty();

		verify(serviceMock).createSubscription(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SUBSCRIBER_ID), any(Subscription.class));
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void deleteSubscription() {
		webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{subscriptionId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID, "subscriptionId", SUBSCRIPTION_ID)))
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(serviceMock).deleteSubscription(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID, SUBSCRIPTION_ID);
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void createSubscriptionWithoutTargetFailsValidation() {
		final var subscription = Subscription.create();

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(subscription)
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}
}

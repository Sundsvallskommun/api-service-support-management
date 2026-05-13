package se.sundsvall.supportmanagement.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.subscription.Subscription;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTarget;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTargetType;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
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

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getSubscriptionsReturnsNotImplemented() {
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void createSubscriptionReturnsNotImplemented() {
		final var subscription = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND).withId("b82bd8ac-1507-4d9a-958d-369261eecc15"));

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(subscription)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void deleteSubscriptionReturnsNotImplemented() {
		webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{subscriptionId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID, "subscriptionId", SUBSCRIPTION_ID)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED);
	}
}

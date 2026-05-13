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
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.api.model.subscriber.Subscriber;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class SubscribersResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/subscribers";
	private static final String NAMESPACE = "my-namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String SUBSCRIBER_ID = "123e4567-e89b-12d3-a456-426614174000";

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getSubscribersReturnsNotImplemented() {
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void getSubscriberReturnsNotImplemented() {
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{subscriberId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void createSubscriberReturnsNotImplemented() {
		final var subscriber = Subscriber.create()
			.withIdentifier(Identifier.create().withType("adAccount").withValue("joe01doe"));

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(subscriber)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void updateSubscriberReturnsNotImplemented() {
		final var subscriber = Subscriber.create()
			.withName("Updated name");

		webTestClient.patch()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{subscriberId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(subscriber)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void deleteSubscriberReturnsNotImplemented() {
		webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{subscriberId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED);
	}
}

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
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.api.model.subscriber.Subscriber;
import se.sundsvall.supportmanagement.service.SubscriberService;

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
class SubscribersResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/subscribers";
	private static final String NAMESPACE = "my-namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String SUBSCRIBER_ID = "123e4567-e89b-12d3-a456-426614174000";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private SubscriberService serviceMock;

	@Test
	void getSubscribersWithoutFilter() {
		final var subscriber = Subscriber.create().withId(SUBSCRIBER_ID).withName("a");
		when(serviceMock.findSubscribers(MUNICIPALITY_ID, NAMESPACE, null, null)).thenReturn(List.of(subscriber));

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Subscriber.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).containsExactly(subscriber);
		verify(serviceMock).findSubscribers(MUNICIPALITY_ID, NAMESPACE, null, null);
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void getSubscribersWithIdentifierFilter() {
		when(serviceMock.findSubscribers(MUNICIPALITY_ID, NAMESPACE, "adAccount", "joe01doe")).thenReturn(List.of());

		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("identifierType", "adAccount")
				.queryParam("identifierValue", "joe01doe")
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk();

		verify(serviceMock).findSubscribers(MUNICIPALITY_ID, NAMESPACE, "adAccount", "joe01doe");
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void getSubscriber() {
		final var subscriber = Subscriber.create()
			.withId(SUBSCRIBER_ID)
			.withName("Servicedesk-bevakning")
			.withIdentifier(Identifier.create().withType("adAccount").withValue("joe01doe"))
			.withCreated(OffsetDateTime.now().minusDays(1))
			.withSubscriptionCount(2);

		when(serviceMock.findSubscriber(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID)).thenReturn(subscriber);

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{subscriberId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Subscriber.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(subscriber);
		verify(serviceMock).findSubscriber(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void createSubscriber() {
		final var newId = "f0882f1d-06bc-47fd-b017-1d8307f5ce95";
		final var subscriber = Subscriber.create()
			.withIdentifier(Identifier.create().withType("adAccount").withValue("joe01doe"))
			.withName("Servicedesk");

		when(serviceMock.createSubscriber(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Subscriber.class))).thenReturn(newId);

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(subscriber)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/subscribers/" + newId)
			.expectBody().isEmpty();

		verify(serviceMock).createSubscriber(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Subscriber.class));
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void updateSubscriber() {
		final var patch = Subscriber.create().withName("Updated name");
		final var updated = Subscriber.create().withId(SUBSCRIBER_ID).withName("Updated name");

		when(serviceMock.updateSubscriber(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SUBSCRIBER_ID), any(Subscriber.class)))
			.thenReturn(updated);

		final var response = webTestClient.patch()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{subscriberId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(patch)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Subscriber.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(updated);
		verify(serviceMock).updateSubscriber(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SUBSCRIBER_ID), any(Subscriber.class));
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void deleteSubscriber() {
		webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{subscriberId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID)))
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(serviceMock).deleteSubscriber(MUNICIPALITY_ID, NAMESPACE, SUBSCRIBER_ID);
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void createSubscriberWithoutIdentifierFailsValidation() {
		final var subscriber = Subscriber.create().withName("missing identifier");

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(subscriber)
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}
}

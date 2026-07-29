package se.sundsvall.supportmanagement.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.SubscriptionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class SubscriptionsResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/subscribers/{subscriberId}/subscriptions";
	private static final String NAMESPACE = "my-namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String SUBSCRIBER_ID = "123e4567-e89b-12d3-a456-426614174000";
	private static final String SUBSCRIPTION_ID = "f0882f1d-06bc-47fd-b017-1d8307f5ce95";
	private static final String INVALID = "#invalid#";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private SubscriptionService serviceMock;

	@Test
	void getSubscriptionsWithInvalidMunicipalityId() {
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "subscriberId", SUBSCRIBER_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("getSubscriptions.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void getSubscriptionsWithInvalidNamespace() {
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("getSubscriptions.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void getSubscriptionsWithInvalidSubscriberId() {
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", INVALID)))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("getSubscriptions.subscriberId", "not a valid UUID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteSubscriptionWithInvalidMunicipalityId() {
		final var response = webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{subscriptionId}").build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID, "subscriberId", SUBSCRIBER_ID, "subscriptionId", SUBSCRIPTION_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("deleteSubscription.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteSubscriptionWithInvalidNamespace() {
		final var response = webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{subscriptionId}").build(Map.of("namespace", INVALID, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID, "subscriptionId", SUBSCRIPTION_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("deleteSubscription.namespace", "can only contain A-Z, a-z, 0-9, - and _"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteSubscriptionWithInvalidSubscriberId() {
		final var response = webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{subscriptionId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", INVALID, "subscriptionId", SUBSCRIPTION_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("deleteSubscription.subscriberId", "not a valid UUID"));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteSubscriptionWithInvalidSubscriptionId() {
		final var response = webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{subscriptionId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID, "subscriptionId", INVALID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("deleteSubscription.subscriptionId", "not a valid UUID"));

		verifyNoInteractions(serviceMock);
	}
}

package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannel;
import se.sundsvall.supportmanagement.api.model.subscriber.Subscriber;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.SubscriberService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType.SMS;

@ResourceTest
class SubscribersResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/subscribers";
	private static final String NAMESPACE = "my-namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String SUBSCRIBER_ID = "123e4567-e89b-12d3-a456-426614174000";
	private static final String INVALID_MOBILE_NUMBER = "070-1234567";
	private static final String SMS_DESTINATION_MESSAGE = "destination of an SMS channel must be a valid MSISDN, e.g. +46701234567";

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private SubscriberService serviceMock;

	@Autowired
	private AccessControlService accessControlServiceMock;

	@AfterEach
	void verifyNoCalls() {
		verifyNoInteractions(serviceMock, accessControlServiceMock);
	}

	@Test
	void createSubscriberWithInvalidSmsDestination() {
		final var subscriber = Subscriber.create()
			.withIdentifier(Identifier.create().withType("adAccount").withValue("joe01doe"))
			.withName("Servicedesk")
			.withChannels(List.of(NotificationChannel.create().withType(SMS).withDestination(INVALID_MOBILE_NUMBER)));

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(subscriber)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("channels[0]", SMS_DESTINATION_MESSAGE));
	}

	@Test
	void updateSubscriberWithInvalidSmsDestination() {
		final var patch = Subscriber.create()
			.withChannels(List.of(NotificationChannel.create().withType(SMS).withDestination(INVALID_MOBILE_NUMBER)));

		final var response = webTestClient.patch()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{subscriberId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "subscriberId", SUBSCRIBER_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(patch)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("channels[0]", SMS_DESTINATION_MESSAGE));
	}
}

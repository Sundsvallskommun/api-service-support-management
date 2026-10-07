package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
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
import se.sundsvall.supportmanagement.api.model.subscriber.EventFilter;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionProfile;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.SubscriptionProfileService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class SubscriptionProfilesResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/subscription-profiles";
	private static final String NAMESPACE = "my-namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PROFILE_ID = "123e4567-e89b-12d3-a456-426614174000";
	private static final String INVALID_MUNICIPALITY_ID = "bad-municipality-id";
	private static final String INVALID_UUID = "not-a-valid-uuid";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private SubscriptionProfileService serviceMock;

	@MockitoBean
	private AccessControlService accessControlServiceMock;

	@AfterEach
	void verifyNoCalls() {
		verifyNoInteractions(serviceMock, accessControlServiceMock);
	}

	private ConstraintViolationProblem post(final SubscriptionProfile body) {
		return webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();
	}

	@Test
	void getSubscriptionProfilesWithInvalidMunicipalityId() {
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", INVALID_MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("getSubscriptionProfiles.municipalityId", "not a valid municipality ID"));
	}

	@Test
	void getSubscriptionProfileWithInvalidId() {
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{profileId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "profileId", INVALID_UUID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("getSubscriptionProfile.profileId", "not a valid UUID"));
	}

	@Test
	void createSubscriptionProfileWithoutRequiredFields() {
		final var response = post(SubscriptionProfile.create());

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(
				tuple("createSubscriptionProfile.subscriptionProfile.name", "must not be blank"),
				tuple("createSubscriptionProfile.subscriptionProfile.eventFilters", "must not be empty"),
				tuple("createSubscriptionProfile.subscriptionProfile.channels", "must not be empty"));
	}

	@Test
	void createSubscriptionProfileWithInvalidEventFilter() {
		final var response = post(SubscriptionProfile.create()
			.withName("Mejl")
			.withEventFilters(List.of(EventFilter.create().withType("UNKNOWN")))
			.withChannels(List.of(NotificationChannelType.EMAIL)));

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("eventFilters[0].type", "type must be one of CREATE, READ, UPDATE, DELETE, ACCESS, EXECUTE, CANCEL, DROP"));
	}

	@Test
	void createSubscriptionProfileWithId() {
		final var response = post(SubscriptionProfile.create()
			.withId(PROFILE_ID)
			.withName("Mejl")
			.withEventFilters(List.of(EventFilter.create().withType("CREATE")))
			.withChannels(List.of(NotificationChannelType.EMAIL)));

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("createSubscriptionProfile.subscriptionProfile.id", "must be null"));
	}
}

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
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.SubscriptionProfileMemberService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class SubscriptionProfileMembersResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/subscription-profiles/{profileId}/members";
	private static final String NAMESPACE = "my-namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PROFILE_ID = "123e4567-e89b-12d3-a456-426614174000";
	private static final String INVALID_UUID = "not-a-valid-uuid";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private SubscriptionProfileMemberService serviceMock;

	@MockitoBean
	private AccessControlService accessControlServiceMock;

	@AfterEach
	void verifyNoCalls() {
		verifyNoInteractions(serviceMock, accessControlServiceMock);
	}

	@Test
	void getSubscriptionProfileMembersWithInvalidProfileId() {
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "profileId", INVALID_UUID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactly(tuple("getSubscriptionProfileMembers.profileId", "not a valid UUID"));
	}

	@Test
	void replaceSubscriptionProfileMembersWithInvalidIdentifier() {
		final var response = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "profileId", PROFILE_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(List.of(Identifier.create().withType("email").withValue("")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(
				tuple("replaceSubscriptionProfileMembers.members[0].type", "type must be 'adAccount' or 'partyId'"),
				tuple("replaceSubscriptionProfileMembers.members[0].value", "must not be blank"));
	}
}

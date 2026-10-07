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
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.SubscriptionProfileMemberService;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class SubscriptionProfileMembersResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/subscription-profiles/{profileId}/members";
	private static final String NAMESPACE = "my-namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PROFILE_ID = "123e4567-e89b-12d3-a456-426614174000";
	private static final Map<String, String> PATH_PARAMS = Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "profileId", PROFILE_ID);

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private SubscriptionProfileMemberService serviceMock;

	@MockitoBean
	private AccessControlService accessControlServiceMock;

	@AfterEach
	void verifyNoMore() {
		verifyNoMoreInteractions(serviceMock, accessControlServiceMock);
	}

	@Test
	void getSubscriptionProfileMembers() {
		final var members = List.of(Identifier.create().withType("adAccount").withValue("anna01"));
		when(serviceMock.findMembers(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID)).thenReturn(members);

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(PATH_PARAMS))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(Identifier.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(members);
		verify(serviceMock).findMembers(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID);
	}

	@Test
	void replaceSubscriptionProfileMembers() {
		final var members = List.of(
			Identifier.create().withType("adAccount").withValue("anna01"),
			Identifier.create().withType("adAccount").withValue("bert02"));

		webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(PATH_PARAMS))
			.contentType(APPLICATION_JSON)
			.bodyValue(members)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.SUBSCRIPTION_PROFILE, RW);
		verify(serviceMock).syncMembers(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID, members);
	}

	@Test
	void replaceSubscriptionProfileMembersWithEmptyList() {
		webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(PATH_PARAMS))
			.contentType(APPLICATION_JSON)
			.bodyValue(List.of())
			.exchange()
			.expectStatus().isNoContent();

		verify(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.SUBSCRIPTION_PROFILE, RW);
		verify(serviceMock).syncMembers(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID, List.of());
	}
}

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
import se.sundsvall.supportmanagement.api.model.subscriber.EventFilter;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionProfile;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.SubscriptionProfileService;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class SubscriptionProfilesResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/subscription-profiles";
	private static final String NAMESPACE = "my-namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PROFILE_ID = "123e4567-e89b-12d3-a456-426614174000";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private SubscriptionProfileService serviceMock;

	@MockitoBean
	private AccessControlService accessControlServiceMock;

	@AfterEach
	void verifyNoMore() {
		verifyNoMoreInteractions(serviceMock, accessControlServiceMock);
	}

	private static SubscriptionProfile profile() {
		return SubscriptionProfile.create()
			.withName("Mejl om nya ärenden")
			.withEventFilters(List.of(EventFilter.create().withType("CREATE").withSubtype("ERRAND")))
			.withChannels(List.of(NotificationChannelType.EMAIL));
	}

	@Test
	void getSubscriptionProfiles() {
		final var profile = profile().withId(PROFILE_ID);
		when(serviceMock.findSubscriptionProfiles(MUNICIPALITY_ID, NAMESPACE)).thenReturn(List.of(profile));

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(SubscriptionProfile.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).containsExactly(profile);
		verify(serviceMock).findSubscriptionProfiles(MUNICIPALITY_ID, NAMESPACE);
	}

	@Test
	void getSubscriptionProfile() {
		final var profile = profile().withId(PROFILE_ID);
		when(serviceMock.findSubscriptionProfile(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID)).thenReturn(profile);

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{profileId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "profileId", PROFILE_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(SubscriptionProfile.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(profile);
		verify(serviceMock).findSubscriptionProfile(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID);
	}

	@Test
	void createSubscriptionProfile() {
		when(serviceMock.createSubscriptionProfile(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(SubscriptionProfile.class))).thenReturn(PROFILE_ID);

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(profile())
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/subscription-profiles/" + PROFILE_ID)
			.expectBody().isEmpty();

		verify(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.SUBSCRIPTION_PROFILE, RW);
		verify(serviceMock).createSubscriptionProfile(MUNICIPALITY_ID, NAMESPACE, profile());
	}

	@Test
	void createSubscriptionProfileWithoutChannels() {
		final var profile = profile().withChannels(null);
		when(serviceMock.createSubscriptionProfile(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(SubscriptionProfile.class))).thenReturn(PROFILE_ID);

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(profile)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/subscription-profiles/" + PROFILE_ID);

		verify(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.SUBSCRIPTION_PROFILE, RW);
		verify(serviceMock).createSubscriptionProfile(MUNICIPALITY_ID, NAMESPACE, profile);
	}

	@Test
	void updateSubscriptionProfile() {
		final var patch = SubscriptionProfile.create().withDescription("Ny beskrivning");
		final var updated = profile().withId(PROFILE_ID).withDescription("Ny beskrivning");
		when(serviceMock.updateSubscriptionProfile(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(PROFILE_ID), any(SubscriptionProfile.class))).thenReturn(updated);

		final var response = webTestClient.patch()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{profileId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "profileId", PROFILE_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(patch)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(SubscriptionProfile.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(updated);
		verify(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.SUBSCRIPTION_PROFILE, RW);
		verify(serviceMock).updateSubscriptionProfile(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID, patch);
	}

	@Test
	void deleteSubscriptionProfile() {
		webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(PATH + "/{profileId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "profileId", PROFILE_ID)))
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(accessControlServiceMock).verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.SUBSCRIPTION_PROFILE, RW);
		verify(serviceMock).deleteSubscriptionProfile(MUNICIPALITY_ID, NAMESPACE, PROFILE_ID);
	}
}

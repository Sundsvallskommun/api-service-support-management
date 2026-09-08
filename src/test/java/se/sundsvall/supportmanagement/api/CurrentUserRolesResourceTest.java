package se.sundsvall.supportmanagement.api;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.service.AccessControlService;

import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class CurrentUserRolesResourceTest {

	@MockitoBean
	private AccessControlService accessControlService;
	@Autowired
	private WebTestClient webTestClient;

	@Test
	void exposesRoleIdsKeysAndDisplayNamesWithoutCachingUserGrants() {
		final var role = Role.create().withId("cc000000-0000-0000-0000-000000000100").withName("MANAGER").withDisplayName("Enhetschef");
		when(accessControlService.findCurrentUserRoles("NAMESPACE", "2281")).thenReturn(List.of(role));
		webTestClient.get().uri("/2281/NAMESPACE/users/me/roles").exchange()
			.expectStatus().isOk().expectHeader().valueEquals("Cache-Control", "private, no-store")
			.expectBodyList(Role.class).isEqualTo(List.of(role));
	}

	@Test
	void missingIdentityIsAnErrorRatherThanAllNamespaceRoles() {
		when(accessControlService.findCurrentUserRoles("NAMESPACE", "2281")).thenThrow(Problem.valueOf(UNAUTHORIZED, "AD account required"));
		webTestClient.get().uri("/2281/NAMESPACE/users/me/roles").exchange().expectStatus().isUnauthorized();
	}
}

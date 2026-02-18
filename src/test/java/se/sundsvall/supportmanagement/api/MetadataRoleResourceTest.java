package se.sundsvall.supportmanagement.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataRoleResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/roles";

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createRole() {
		// Setup
		final var roleName = "roleName";
		final var role = Role.create().withName(roleName);

		// Mock
		when(metadataServiceMock.createRole(NAMESPACE, MUNICIPALITY_ID, role)).thenReturn(roleName);

		// Call
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(role)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/roles/" + roleName)
			.expectBody().isEmpty();

		// Verifications & assertions
		verify(metadataServiceMock).createRole(NAMESPACE, MUNICIPALITY_ID, role);
	}

	@Test
	void getRole() {
		// Setup
		final var roleName = "roleName";
		final var role = Role.create().withName(roleName);

		// Mock
		when(metadataServiceMock.getRole(NAMESPACE, MUNICIPALITY_ID, roleName)).thenReturn(role);

		// Call
		final var response = webTestClient.get().uri(builder -> builder.path(PATH + "/{role}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "role", roleName)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(Role.class)
			.returnResult()
			.getResponseBody();

		// Verifications & assertions
		verify(metadataServiceMock).getRole(NAMESPACE, MUNICIPALITY_ID, roleName);
		assertThat(response).isNotNull().isEqualTo(role);
	}

	@Test
	void getRoles() {
		// Call
		webTestClient.get().uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(String[].class)
			.isEqualTo(EMPTY_STRING_ARRAY);

		// Verifications & assertions
		verify(metadataServiceMock).findRoles(NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void deleteRole() {
		// Setup
		final var roleName = "roleName";

		// Call
		webTestClient.delete().uri(builder -> builder.path(PATH + "/{role}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "role", roleName)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

		// Verifications & assertions
		verify(metadataServiceMock).deleteRole(NAMESPACE, MUNICIPALITY_ID, roleName);
	}

}

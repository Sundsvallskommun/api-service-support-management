package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.service.MetadataService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MetadataContactReasonResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/metadata/contactreasons";

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createContactReason() {
		final var body = ContactReason.create()
			.withId(1L)
			.withReason("reason")
			.withCreated(OffsetDateTime.now())
			.withModified(OffsetDateTime.now());

		when(metadataServiceMock.createContactReason(NAMESPACE, MUNICIPALITY_ID, body)).thenReturn(body.getId());

		webTestClient.post().uri(builder -> builder.path(PATH).build(MUNICIPALITY_ID, NAMESPACE))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/contactreasons/" + body.getId())
			.expectBody().isEmpty();
	}

	@Test
	void getContactReason() {

		final var contactReasonId = 1L;
		final var reason = "reason";
		final var contactReason = ContactReason.create()
			.withId(contactReasonId)
			.withReason(reason);

		when(metadataServiceMock.getContactReasonByIdAndNamespaceAndMunicipalityId(contactReasonId, NAMESPACE, MUNICIPALITY_ID)).thenReturn(contactReason);

		final var result = webTestClient.get().uri(builder -> builder.path(PATH + "/{contactReasonId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "contactReasonId", contactReasonId)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(ContactReason.class)
			.isEqualTo(contactReason)
			.returnResult()
			.getResponseBody();

		assertThat(result.getReason()).isNotNull().isEqualTo(reason);
	}

	@Test
	void getContactReasons() {

		final var reason = "reason";
		final var contactReason1 = ContactReason.create().withReason("reason1");
		final var contactReason2 = ContactReason.create().withReason("reason2");
		final var contactReasons = List.of(contactReason1, contactReason2);

		when(metadataServiceMock.findContactReasonsForNamespaceAndMunicipality(NAMESPACE, MUNICIPALITY_ID)).thenReturn(contactReasons);

		final var result = webTestClient.get().uri(builder -> builder.path(PATH).build(MUNICIPALITY_ID, NAMESPACE, reason))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(ContactReason.class)
			.returnResult()
			.getResponseBody();

		assertThat(result).hasSize(2).extracting(ContactReason::getReason).containsExactly("reason1", "reason2");
	}

	@Test
	void updateContactReason() {

		final var contactReasonId = 1L;
		final var reason = "reason";
		final var patch = ContactReason.create()
			.withReason(reason)
			.withId(contactReasonId);

		when(metadataServiceMock.patchContactReason(contactReasonId, NAMESPACE, MUNICIPALITY_ID, patch)).thenReturn(patch);

		webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{contactReasonId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "contactReasonId", contactReasonId)))
			.contentType(APPLICATION_JSON)
			.bodyValue(patch)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.OK);

		verify(metadataServiceMock).patchContactReason(contactReasonId, NAMESPACE, MUNICIPALITY_ID, patch);
	}

	@Test
	void deleteContactReason() {

		final var contactReasonId = 1L;

		webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{contactReasonId}").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "contactReasonId", contactReasonId)))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

		verify(metadataServiceMock).deleteContactReason(contactReasonId, NAMESPACE, MUNICIPALITY_ID);
	}
}

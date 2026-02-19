package se.sundsvall.supportmanagement.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class NamespaceConfigResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/namespace-config";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String DISPLAY_NAME = "DisplayName";
	private static final String SHORT_CODE = "NS";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private NamespaceConfigService serviceMock;

	@Test
	void create() {
		final var namespaceConfig = NamespaceConfig.create()
			.withDisplayName(DISPLAY_NAME)
			.withShortCode(SHORT_CODE);

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/namespace-config")
			.expectBody().isEmpty();

		verify(serviceMock).create(namespaceConfig, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void read() {
		final var namespaceConfig = NamespaceConfig.create()
			.withDisplayName(DISPLAY_NAME)
			.withShortCode(SHORT_CODE)
			.withCreated(OffsetDateTime.now().minusDays(2))
			.withModified(OffsetDateTime.now().minusDays(1));

		when(serviceMock.get(any(), any())).thenReturn(namespaceConfig);

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(NamespaceConfig.class)
			.returnResult()
			.getResponseBody();

		verify(serviceMock).get(NAMESPACE, MUNICIPALITY_ID);
		assertThat(response).isNotNull().isEqualTo(namespaceConfig);
	}

	@Test
	void readAll() {
		final var configs = List.of(NamespaceConfig.create()
			.withDisplayName(DISPLAY_NAME)
			.withShortCode(SHORT_CODE)
			.withCreated(OffsetDateTime.now().minusDays(2))
			.withModified(OffsetDateTime.now().minusDays(1)));

		when(serviceMock.findAll(any())).thenReturn(configs);

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/namespace-configs").build())
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(NamespaceConfig.class)
			.returnResult()
			.getResponseBody();

		verify(serviceMock).findAll(null);
		assertThat(response).isNotNull().isEqualTo(configs);
	}

	@Test
	void readAllWithMunicipalityId() {
		final var configs = List.of(NamespaceConfig.create()
			.withDisplayName(DISPLAY_NAME)
			.withShortCode(SHORT_CODE)
			.withCreated(OffsetDateTime.now().minusDays(2))
			.withModified(OffsetDateTime.now().minusDays(1)));

		when(serviceMock.findAll(any())).thenReturn(configs);

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/namespace-configs").queryParam("municipalityId", MUNICIPALITY_ID).build())
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(NamespaceConfig.class)
			.returnResult()
			.getResponseBody();

		verify(serviceMock).findAll(MUNICIPALITY_ID);
		assertThat(response).isNotNull().isEqualTo(configs);
	}

	@Test
	void update() {
		final var namespaceConfig = NamespaceConfig.create()
			.withDisplayName(DISPLAY_NAME)
			.withShortCode(SHORT_CODE);

		webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(namespaceConfig)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(serviceMock).replace(namespaceConfig, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void delete() {
		webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(serviceMock).delete(NAMESPACE, MUNICIPALITY_ID);
	}
}

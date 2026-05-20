package se.sundsvall.supportmanagement.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.config.MessageExchangeIntegration;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.service.MetadataService;
import se.sundsvall.supportmanagement.service.config.MessageExchangeIntegrationConfigService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MessageExchangeIntegrationConfigResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/messageexchange-integration-config";

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private MessageExchangeIntegrationConfigService serviceMock;

	@MockitoBean
	private MetadataService metadataServiceMock;

	@BeforeEach
	void setup() {
		when(metadataServiceMock.findStatuses(any(), any(), any(Sort.class)))
			.thenReturn(List.of(Status.create().withName("SOLVED"), Status.create().withName("OPEN")));
	}

	@Test
	void create() {
		final var config = MessageExchangeIntegration.create()
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("OPEN");

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(config)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/messageexchange-integration-config")
			.expectBody().isEmpty();

		verify(serviceMock).create(config, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void read() {
		final var config = MessageExchangeIntegration.create()
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("OPEN")
			.withCreated(OffsetDateTime.now().minusDays(2))
			.withModified(OffsetDateTime.now().minusDays(1));

		when(serviceMock.get(any(), any())).thenReturn(config);

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(MessageExchangeIntegration.class)
			.returnResult()
			.getResponseBody();

		verify(serviceMock).get(NAMESPACE, MUNICIPALITY_ID);
		assertThat(response).isNotNull().isEqualTo(config);
	}

	@Test
	void update() {
		final var config = MessageExchangeIntegration.create()
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("OPEN");

		webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(config)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(serviceMock).replace(config, NAMESPACE, MUNICIPALITY_ID);
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

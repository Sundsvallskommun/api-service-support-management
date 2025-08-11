package se.sundsvall.supportmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.config.MessageExchangeSync;
import se.sundsvall.supportmanagement.service.config.MessageExchangeSyncConfigService;

@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class MessageExchangeSyncConfigResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final Long CONFIG_ID = 1L;

	private static final String BASE_PATH = "/{municipalityId}/message-exchange-sync-config";
	private static final String PATH_WITH_ID = BASE_PATH + "/{id}";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private MessageExchangeSyncConfigService serviceMock;

	@Test
	void create() {

		final var syncConfig = new MessageExchangeSync()
			.withActive(true)
			.withNamespace("namespace");
		when(serviceMock.create(any(MessageExchangeSync.class), eq(MUNICIPALITY_ID)))
			.thenReturn(CONFIG_ID);

		webTestClient.post()
			.uri(BASE_PATH, MUNICIPALITY_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(syncConfig)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().valueEquals("Location", "/" + MUNICIPALITY_ID + "/message-exchange-sync-config/" + CONFIG_ID);

		verify(serviceMock).create(syncConfig, MUNICIPALITY_ID);
	}

	@Test
	void getAllByMunicipalityId() {

		final var syncConfig = new MessageExchangeSync();
		final var expectedList = List.of(syncConfig);
		when(serviceMock.getAllByMunicipalityId(MUNICIPALITY_ID)).thenReturn(expectedList);

		final var response = webTestClient.get()
			.uri(BASE_PATH, MUNICIPALITY_ID)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(MessageExchangeSync.class)
			.returnResult()
			.getResponseBody();

		verify(serviceMock).getAllByMunicipalityId(MUNICIPALITY_ID);
		assertThat(response).isNotNull().hasSize(1).first().isEqualTo(syncConfig);
	}

	@Test
	void update() {
		final var syncConfig = new MessageExchangeSync()
			.withActive(true)
			.withNamespace("namespace");

		webTestClient.put()
			.uri(PATH_WITH_ID, MUNICIPALITY_ID, CONFIG_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(syncConfig)
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).replace(syncConfig, MUNICIPALITY_ID, CONFIG_ID);
	}

	@Test
	void delete() {

		webTestClient.delete()
			.uri(PATH_WITH_ID, MUNICIPALITY_ID, CONFIG_ID)
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).delete(MUNICIPALITY_ID, CONFIG_ID);
	}
}

package se.sundsvall.supportmanagement.api;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.service.ErrandService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ResourceTest
class ErrandSoftDeleteResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = UUID.randomUUID().toString();
	private static final String IF_MATCH = "\"3\"";

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private ErrandService errandServiceMock;

	@Test
	void softDeleteErrand() {
		webTestClient.post()
			.uri(builder -> builder.path(PATH + "/soft-delete").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(errandServiceMock).softDeleteErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, null);
		verifyNoMoreInteractions(errandServiceMock);
	}

	@Test
	void softDeleteErrandWithIfMatch() {
		webTestClient.post()
			.uri(builder -> builder.path(PATH + "/soft-delete").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.header("If-Match", IF_MATCH)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(errandServiceMock).softDeleteErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, IF_MATCH);
		verifyNoMoreInteractions(errandServiceMock);
	}

	@Test
	void restoreErrand() {
		webTestClient.post()
			.uri(builder -> builder.path(PATH + "/restore").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID)))
			.header("If-Match", IF_MATCH)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(errandServiceMock).restoreErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, IF_MATCH);
		verifyNoMoreInteractions(errandServiceMock);
	}
}

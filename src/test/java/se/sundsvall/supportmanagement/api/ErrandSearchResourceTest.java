package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.service.search.ErrandSearchService;
import se.sundsvall.supportmanagement.service.search.index.ErrandReindexService;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandSearchResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/errands/search";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandSearchService searchServiceMock;

	@MockitoBean
	private ErrandReindexService reindexServiceMock;

	@Test
	void searchErrands() {
		final var pageable = PageRequest.of(2, 5, Sort.by("created").descending());
		final var errand = Errand.create().withId("id").withErrandNumber("KC-1");
		when(searchServiceMock.search(NAMESPACE, MUNICIPALITY_ID, "vatten status:new", pageable)).thenReturn(new PageImpl<>(List.of(errand), pageable, 11));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH)
				.queryParam("query", "vatten status:new")
				.queryParam("page", 2)
				.queryParam("size", 5)
				.queryParam("sort", "created,desc")
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(JsonNode.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.path("totalElements").asInt()).isEqualTo(11);
		assertThat(response.path("content")).hasSize(1);
		assertThat(response.path("content").get(0).path("errandNumber").asString()).isEqualTo("KC-1");
		verify(searchServiceMock).search(NAMESPACE, MUNICIPALITY_ID, "vatten status:new", pageable);
		verifyNoMoreInteractions(searchServiceMock);
		verifyNoInteractions(reindexServiceMock);
	}

	@Test
	void searchErrandsWithoutQuery() {
		final var pageable = PageRequest.of(0, 20);
		when(searchServiceMock.search(NAMESPACE, MUNICIPALITY_ID, null, pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

		webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON);

		verify(searchServiceMock).search(NAMESPACE, MUNICIPALITY_ID, null, pageable);
	}

	@Test
	void reindexErrands() {
		webTestClient.post()
			.uri(builder -> builder.path(PATH + "/reindex").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isAccepted()
			.expectBody().isEmpty();

		verify(reindexServiceMock).reindex(NAMESPACE, MUNICIPALITY_ID, false);
		verifyNoInteractions(searchServiceMock);
	}

	@Test
	void reindexErrandsFully() {
		webTestClient.post()
			.uri(builder -> builder.path(PATH + "/reindex").queryParam("full", true).build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isAccepted()
			.expectBody().isEmpty();

		verify(reindexServiceMock).reindex(NAMESPACE, MUNICIPALITY_ID, true);
	}
}

package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.api.model.errand.CountBucket;
import se.sundsvall.supportmanagement.api.model.errand.CountGroup;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.SearchCountResponse;
import se.sundsvall.supportmanagement.service.search.ErrandSearchService;
import se.sundsvall.supportmanagement.service.search.index.ErrandReindexService;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ResourceTest
class ErrandSearchResourceTest {

	private static final String PATH = "/{municipalityId}/{namespace}/errands/search";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private ErrandSearchService searchServiceMock;

	@Autowired
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
	void countErrands() {
		when(searchServiceMock.count(NAMESPACE, MUNICIPALITY_ID, "status:new", null)).thenReturn(SearchCountResponse.of(137));

		webTestClient.get()
			.uri(builder -> builder.path(PATH + "/count").queryParam("query", "status:new").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody()
			.jsonPath("$.count").isEqualTo(137)
			.jsonPath("$.group").doesNotExist();

		verify(searchServiceMock).count(NAMESPACE, MUNICIPALITY_ID, "status:new", null);
		verifyNoMoreInteractions(searchServiceMock);
	}

	@Test
	void countErrandsGrouped() {
		final var group = new CountGroup("status", List.of(new CountBucket("NEW", 91), new CountBucket("ONGOING", 46)), 0, 0);
		when(searchServiceMock.count(NAMESPACE, MUNICIPALITY_ID, null, "status")).thenReturn(new SearchCountResponse(137, group));

		webTestClient.get()
			.uri(builder -> builder.path(PATH + "/count").queryParam("groupBy", "status").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.count").isEqualTo(137)
			.jsonPath("$.group.property").isEqualTo("status")
			.jsonPath("$.group.buckets[0].value").isEqualTo("NEW")
			.jsonPath("$.group.buckets[0].count").isEqualTo(91)
			.jsonPath("$.group.withoutValue").isEqualTo(0)
			.jsonPath("$.group.withheld").isEqualTo(0);

		verify(searchServiceMock).count(NAMESPACE, MUNICIPALITY_ID, null, "status");
	}

	/** Neither has any meaning for a count, and being told so is of no use to a client that sent them by habit. */
	@Test
	void countErrandsIgnoresSortingAndPaging() {
		when(searchServiceMock.count(NAMESPACE, MUNICIPALITY_ID, null, null)).thenReturn(SearchCountResponse.of(4));

		webTestClient.get()
			.uri(builder -> builder.path(PATH + "/count")
				.queryParam("sort", "created,desc")
				.queryParam("page", 3)
				.queryParam("size", 50)
				.build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.count").isEqualTo(4);

		verify(searchServiceMock).count(NAMESPACE, MUNICIPALITY_ID, null, null);
	}

	@Test
	void reindexErrands() {
		webTestClient.post()
			.uri(builder -> builder.path(PATH + "/reindex").build(Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isAccepted()
			.expectBody().isEmpty();

		verify(reindexServiceMock).reindex(NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(searchServiceMock);
	}

	/**
	 * The whole index belongs to no namespace, so it is asked for outside of one.
	 */
	@Test
	void reindexEverything() {
		webTestClient.post()
			.uri("/search/reindex")
			.exchange()
			.expectStatus().isAccepted()
			.expectBody().isEmpty();

		verify(reindexServiceMock).reindexEverything();
	}
}

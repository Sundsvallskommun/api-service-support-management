package se.sundsvall.supportmanagement.integration.elasticsearch;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonParameterSearchServiceTest {

	@Mock
	private ElasticsearchOperations elasticsearchOperationsMock;

	@Mock
	private SearchHits<JsonParameterDocument> searchHitsMock;

	@InjectMocks
	private JsonParameterSearchService service;

	@Test
	void findErrandIdsReturnsMatches() {
		// Setup
		final var doc1 = JsonParameterDocument.create().withErrandId("errand-1");
		final var doc2 = JsonParameterDocument.create().withErrandId("errand-2");
		final var hit1 = new SearchHit<>("idx", "1", null, Float.NaN, null, null, null, null, null, null, doc1);
		final var hit2 = new SearchHit<>("idx", "2", null, Float.NaN, null, null, null, null, null, null, doc2);

		// Mock
		when(elasticsearchOperationsMock.search(any(NativeQuery.class), eq(JsonParameterDocument.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(List.of(hit1, hit2));

		// Call
		final var result = service.findErrandIdsByJsonParameterValues("namespace", "municipalityId", "key", Map.of("city", "Sundsvall"));

		// Verify
		assertThat(result).containsExactly("errand-1", "errand-2");
		verify(elasticsearchOperationsMock).search(any(NativeQuery.class), eq(JsonParameterDocument.class));
	}

	@Test
	void findErrandIdsNoMatches() {
		// Mock
		when(elasticsearchOperationsMock.search(any(NativeQuery.class), eq(JsonParameterDocument.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(List.of());

		// Call
		final var result = service.findErrandIdsByJsonParameterValues("namespace", "municipalityId", null, Map.of("city", "Sundsvall"));

		// Verify
		assertThat(result).isEmpty();
	}

	@Test
	void findErrandIdsDeduplicates() {
		// Setup - two hits with same errand ID
		final var doc1 = JsonParameterDocument.create().withErrandId("errand-1");
		final var doc2 = JsonParameterDocument.create().withErrandId("errand-1");
		final var hit1 = new SearchHit<>("idx", "1", null, Float.NaN, null, null, null, null, null, null, doc1);
		final var hit2 = new SearchHit<>("idx", "2", null, Float.NaN, null, null, null, null, null, null, doc2);

		// Mock
		when(elasticsearchOperationsMock.search(any(NativeQuery.class), eq(JsonParameterDocument.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(List.of(hit1, hit2));

		// Call
		final var result = service.findErrandIdsByJsonParameterValues("namespace", "municipalityId", "key", Map.of("city", "Sundsvall"));

		// Verify
		assertThat(result).containsExactly("errand-1");
	}
}

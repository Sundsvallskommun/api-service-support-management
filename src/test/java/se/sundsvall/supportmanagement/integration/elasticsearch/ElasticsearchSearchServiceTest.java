package se.sundsvall.supportmanagement.integration.elasticsearch;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import se.sundsvall.supportmanagement.integration.elasticsearch.model.JsonParameterDocument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticsearchSearchServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String QUERY_STRING = "\"FAC-0001\"";

	@Mock
	private ElasticsearchOperations elasticsearchOperationsMock;

	@Mock
	private SearchHits<JsonParameterDocument> searchHitsMock;

	@Mock
	private SearchHit<JsonParameterDocument> firstSearchHitMock;

	@Mock
	private SearchHit<JsonParameterDocument> secondSearchHitMock;

	@InjectMocks
	private ElasticsearchSearchService service;

	@Captor
	private ArgumentCaptor<NativeQuery> nativeQueryCaptor;

	@Test
	void query() {
		when(elasticsearchOperationsMock.search(any(NativeQuery.class), eq(JsonParameterDocument.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.stream()).thenReturn(Stream.of(firstSearchHitMock, secondSearchHitMock));
		when(firstSearchHitMock.getId()).thenReturn("id-1");
		when(secondSearchHitMock.getId()).thenReturn("id-2");

		final var result = service.query(NAMESPACE, MUNICIPALITY_ID, QUERY_STRING);

		assertThat(result).containsExactly("id-1", "id-2");
		verify(elasticsearchOperationsMock).search(nativeQueryCaptor.capture(), eq(JsonParameterDocument.class));
		assertThat(nativeQueryCaptor.getValue().getMaxResults()).isEqualTo(10_000);
		verifyNoMoreInteractions(elasticsearchOperationsMock);
	}

	@Test
	void queryWithoutMatches() {
		when(elasticsearchOperationsMock.search(any(NativeQuery.class), eq(JsonParameterDocument.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.stream()).thenReturn(Stream.empty());

		final var result = service.query(NAMESPACE, MUNICIPALITY_ID, QUERY_STRING);

		assertThat(result).isEmpty();
		verify(elasticsearchOperationsMock).search(any(NativeQuery.class), eq(JsonParameterDocument.class));
		verifyNoMoreInteractions(elasticsearchOperationsMock);
	}

	@Test
	void queryPropagatesElasticsearchFailure() {
		when(elasticsearchOperationsMock.search(any(NativeQuery.class), eq(JsonParameterDocument.class))).thenThrow(new RuntimeException("Elasticsearch down"));

		assertThatThrownBy(() -> service.query(NAMESPACE, MUNICIPALITY_ID, QUERY_STRING))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("Elasticsearch down");
	}
}

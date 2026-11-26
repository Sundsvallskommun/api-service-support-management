package se.sundsvall.supportmanagement.service.search.index;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import org.apache.http.HttpHost;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.message.BasicStatusLine;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.metamodel.ElasticsearchIndexDescriptor;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenSearchClientTest {

	@Mock
	private EntityManagerFactory entityManagerFactoryMock;

	@Mock
	private RestClient restClientMock;

	private OpenSearchClient client() {
		final var client = spy(new OpenSearchClient(entityManagerFactoryMock));
		doReturn(restClientMock).when(client).restClient();
		doReturn("errand-write").when(client).errandWriteIndex();
		return client;
	}

	private static Response responseWith(final int status) {
		final var response = mock(Response.class);
		when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), status, "status"));
		return response;
	}

	private static ResponseException responseExceptionWith(final int status) throws IOException {
		final var response = mock(Response.class);
		when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), status, "status"));
		when(response.getRequestLine()).thenReturn(new BasicRequestLine("HEAD", "/errand-write", new ProtocolVersion("HTTP", 1, 1)));
		when(response.getHost()).thenReturn(new HttpHost("localhost", 9200));
		return new ResponseException(response);
	}

	@Test
	void theIndexIsThereWhenTheClusterSaysSo() throws IOException {
		final var client = client();
		final var response = responseWith(200);
		when(restClientMock.performRequest(any())).thenReturn(response);

		assertThat(client.errandIndexExists()).isTrue();

		final var request = ArgumentCaptor.forClass(Request.class);
		verify(restClientMock).performRequest(request.capture());
		assertThat(request.getValue().getMethod()).isEqualTo("HEAD");
		assertThat(request.getValue().getEndpoint()).isEqualTo("/errand-write");
	}

	@Test
	void theIndexIsNotThereWhenTheClusterDoesNotKnowIt() throws IOException {
		final var client = client();
		final var notFound = responseExceptionWith(404);
		when(restClientMock.performRequest(any())).thenThrow(notFound);

		assertThat(client.errandIndexExists()).isFalse();
	}

	@Test
	void anyOtherAnswerOfTheClusterIsAFailure() throws IOException {
		final var client = client();
		final var unavailable = responseExceptionWith(503);
		when(restClientMock.performRequest(any())).thenThrow(unavailable);

		final var e = assertThrows(ResponseException.class, client::errandIndexExists);

		assertThat(e).isSameAs(unavailable);
	}

	@Test
	void anAnswerOtherThanFoundIsNoIndex() throws IOException {
		final var client = client();
		final var response = responseWith(204);
		when(restClientMock.performRequest(any())).thenReturn(response);

		assertThat(client.errandIndexExists()).isFalse();
	}

	@Test
	void theClientAndTheIndexAreReachedThroughHibernateSearch() {
		final var mapping = mock(SearchMapping.class);
		final var backend = mock(Backend.class);
		final var elasticsearchBackend = mock(ElasticsearchBackend.class);
		final SearchIndexedEntity<ErrandEntity> indexedEntity = mock();
		final var indexManager = mock(IndexManager.class);
		final var elasticsearchIndexManager = mock(ElasticsearchIndexManager.class);
		final var elasticsearchDescriptor = mock(ElasticsearchIndexDescriptor.class);
		final var descriptor = mock(IndexDescriptor.class);
		when(mapping.backend()).thenReturn(backend);
		when(backend.unwrap(ElasticsearchBackend.class)).thenReturn(elasticsearchBackend);
		when(elasticsearchBackend.client(RestClient.class)).thenReturn(restClientMock);
		when(mapping.indexedEntity(ErrandEntity.class)).thenReturn(indexedEntity);
		when(indexedEntity.indexManager()).thenReturn(indexManager);
		when(indexManager.unwrap(ElasticsearchIndexManager.class)).thenReturn(elasticsearchIndexManager);
		when(elasticsearchIndexManager.descriptor()).thenReturn(elasticsearchDescriptor);
		when(elasticsearchDescriptor.writeName()).thenReturn("errand-write");
		when(indexManager.descriptor()).thenReturn(descriptor);

		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(mapping);
			final var client = new OpenSearchClient(entityManagerFactoryMock);

			assertThat(client.restClient()).isSameAs(restClientMock);
			assertThat(client.errandWriteIndex()).isEqualTo("errand-write");
			assertThat(client.errandIndex()).isSameAs(descriptor);
		}
	}
}

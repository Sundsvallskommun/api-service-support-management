package se.sundsvall.supportmanagement.service.search;

import jakarta.persistence.EntityManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.http.HttpEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Status;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenSearchHealthIndicatorTest {

	@Mock
	private EntityManagerFactory entityManagerFactoryMock;

	@Mock
	private SearchMapping searchMappingMock;

	@Mock
	private Backend backendMock;

	@Mock
	private ElasticsearchBackend elasticsearchBackendMock;

	@Mock
	private RestClient restClientMock;

	@Mock
	private Response responseMock;

	@Mock
	private HttpEntity entityMock;

	private void clusterAnswers(final MockedStatic<Search> search, final String body) throws IOException {
		search.when(() -> Search.mapping(entityManagerFactoryMock)).thenReturn(searchMappingMock);
		when(searchMappingMock.backend()).thenReturn(backendMock);
		when(backendMock.unwrap(ElasticsearchBackend.class)).thenReturn(elasticsearchBackendMock);
		when(elasticsearchBackendMock.client(RestClient.class)).thenReturn(restClientMock);
		when(restClientMock.performRequest(any())).thenReturn(responseMock);
		when(responseMock.getEntity()).thenReturn(entityMock);
		when(entityMock.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes(UTF_8)));
	}

	@Test
	void up() throws IOException {
		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			clusterAnswers(search, "{\"cluster_name\":\"support\",\"status\":\"yellow\"}");

			final var health = new OpenSearchHealthIndicator(entityManagerFactoryMock).health();

			assertThat(health.getStatus()).isEqualTo(Status.UP);
			assertThat(health.getDetails()).containsEntry(OpenSearchHealthIndicator.CLUSTER_NAME, "support").containsEntry(OpenSearchHealthIndicator.CLUSTER_STATUS, "yellow");

			final var request = ArgumentCaptor.forClass(Request.class);
			verify(restClientMock).performRequest(request.capture());
			assertThat(request.getValue().getMethod()).isEqualTo("GET");
			assertThat(request.getValue().getEndpoint()).isEqualTo("/_cluster/health");
		}
	}

	@Test
	void downWhenTheClusterIsRed() throws IOException {
		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			clusterAnswers(search, "{\"cluster_name\":\"support\",\"status\":\"red\"}");

			final var health = new OpenSearchHealthIndicator(entityManagerFactoryMock).health();

			assertThat(health.getStatus()).isEqualTo(Status.DOWN);
			assertThat(health.getDetails()).containsEntry(OpenSearchHealthIndicator.CLUSTER_STATUS, "red");
		}
	}

	@Test
	void downWhenTheClusterCannotBeReached() {
		try (final MockedStatic<Search> search = mockStatic(Search.class)) {
			search.when(() -> Search.mapping(entityManagerFactoryMock)).thenThrow(new IllegalStateException("Connection refused"));

			final var health = new OpenSearchHealthIndicator(entityManagerFactoryMock).health();

			assertThat(health.getStatus()).isEqualTo(Status.DOWN);
			assertThat(health.getDetails()).containsEntry("error", "java.lang.IllegalStateException: Connection refused");
		}
	}
}

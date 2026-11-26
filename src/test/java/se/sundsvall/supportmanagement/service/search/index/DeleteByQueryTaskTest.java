package se.sundsvall.supportmanagement.service.search.index;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.config.SearchProperties;
import tools.jackson.databind.json.JsonMapper;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteByQueryTaskTest {

	private static final String TASK_ID = "node:42";
	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	@Mock
	private OpenSearchClient openSearchMock;

	@Mock
	private RestClient restClientMock;

	private final List<Request> requests = new ArrayList<>();

	private DeleteByQueryTask task(final Duration timeout) {
		return new DeleteByQueryTask(openSearchMock,
			new SearchProperties(10000, Duration.ofSeconds(10), 100, new SearchProperties.Reindex(Duration.ofHours(6), timeout, Duration.ofMillis(1))));
	}

	/** Answers every request in turn with the sent in bodies, the last one for every request after. */
	private void clusterAnswers(final String... bodies) throws IOException {
		when(openSearchMock.restClient()).thenReturn(restClientMock);
		when(restClientMock.performRequest(any())).thenAnswer(invocation -> {
			requests.add(invocation.getArgument(0));
			final var body = bodies[Math.min(requests.size() - 1, bodies.length - 1)];
			final var response = mock(Response.class);
			final var entity = mock(HttpEntity.class);
			if (body != null) {
				lenient().when(response.getEntity()).thenReturn(entity);
				lenient().when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes(UTF_8)));
			}
			return response;
		});
	}

	@Test
	void startsATaskAndWaitsForIt() throws IOException {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		clusterAnswers(
			"{\"task\":\"" + TASK_ID + "\"}",
			"{\"completed\":false,\"task\":{\"status\":{\"deleted\":3}}}",
			"{\"completed\":true,\"task\":{\"status\":{\"deleted\":7}},\"response\":{\"deleted\":7,\"failures\":[]}}",
			null);
		final var progress = new ArrayList<Long>();

		final var removed = task(Duration.ofMinutes(1)).run(MAPPER.readTree("{\"match_all\":{}}"), progress::add);

		assertThat(removed).isEqualTo(7);
		assertThat(progress).containsExactly(3L, 7L);

		final var start = requests.getFirst();
		assertThat(start.getMethod()).isEqualTo("POST");
		assertThat(start.getEndpoint()).isEqualTo("/errand-write/_delete_by_query");
		assertThat(start.getParameters())
			.containsEntry("wait_for_completion", "false")
			.containsEntry("conflicts", "proceed")
			.containsEntry("refresh", "true");
		assertThat(EntityUtils.toString(start.getEntity())).isEqualTo("{\"query\":{\"match_all\":{}}}");

		assertThat(requests.subList(1, 3)).allSatisfy(request -> {
			assertThat(request.getMethod()).isEqualTo("GET");
			assertThat(request.getEndpoint()).isEqualTo("/_tasks/" + TASK_ID);
		});

		// What the cluster kept of the task is removed once it has been read
		assertThat(requests.get(3).getMethod()).isEqualTo("DELETE");
		assertThat(requests.get(3).getEndpoint()).isEqualTo("/.tasks/_doc/" + TASK_ID);
		assertThat(requests).hasSize(4);
	}

	@Test
	void documentsThatCouldNotBeRemovedFailTheRemoval() throws IOException {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		clusterAnswers(
			"{\"task\":\"" + TASK_ID + "\"}",
			"{\"completed\":true,\"response\":{\"deleted\":5,\"failures\":[{\"cause\":{\"reason\":\"shard went away\"}},{}]}}",
			null);
		final var query = MAPPER.readTree("{}");

		final var e = assertThrows(IllegalStateException.class, () -> task(Duration.ofMinutes(1)).run(query, _ -> {}));

		assertThat(e.getMessage()).isEqualTo("The removal of documents from the search index failed: 2 document(s) could not be removed, the first because: shard went away");
	}

	@Test
	void aTaskThatEndedInAnErrorFailsTheRemoval() throws IOException {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		clusterAnswers(
			"{\"task\":\"" + TASK_ID + "\"}",
			"{\"completed\":true,\"error\":{\"type\":\"search_phase_execution_exception\",\"reason\":\"all shards failed\"}}",
			null);
		final var query = MAPPER.readTree("{}");

		final var e = assertThrows(IllegalStateException.class, () -> task(Duration.ofMinutes(1)).run(query, _ -> {}));

		assertThat(e.getMessage()).isEqualTo("The removal of documents from the search index failed: all shards failed");
	}

	@Test
	void aTaskNotDoneInTimeIsCancelled() throws IOException {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		clusterAnswers(
			"{\"task\":\"" + TASK_ID + "\"}",
			"{\"completed\":false,\"task\":{\"status\":{\"deleted\":1}}}",
			"{}");
		final var query = MAPPER.readTree("{}");

		final var e = assertThrows(IllegalStateException.class, () -> task(Duration.ZERO).run(query, _ -> {}));

		assertThat(e.getMessage()).isEqualTo("The removal of documents from the search index was not done within PT0S and was cancelled");
		assertThat(requests.getLast().getMethod()).isEqualTo("POST");
		assertThat(requests.getLast().getEndpoint()).isEqualTo("/_tasks/" + TASK_ID + "/_cancel");
	}

	@Test
	void aClusterThatDoesNotStartTheTaskFailsTheRemoval() throws IOException {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		clusterAnswers("{}");
		final var query = MAPPER.readTree("{}");

		final var e = assertThrows(IllegalStateException.class, () -> task(Duration.ofMinutes(1)).run(query, _ -> {}));

		assertThat(e.getMessage()).isEqualTo(DeleteByQueryTask.NOT_STARTED);
		assertThat(requests).hasSize(1);
	}

	@Test
	void aClusterThatCannotBeReachedFailsTheRemoval() throws IOException {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		when(openSearchMock.restClient()).thenReturn(restClientMock);
		when(restClientMock.performRequest(any())).thenThrow(new IOException("Connection refused"));
		final var query = MAPPER.readTree("{}");

		final var e = assertThrows(RuntimeException.class, () -> task(Duration.ofMinutes(1)).run(query, _ -> {}));

		assertThat(e.getMessage()).isEqualTo(DeleteByQueryTask.NOT_STARTED);
		assertThat(e.getCause()).hasMessage("Connection refused");
	}
}

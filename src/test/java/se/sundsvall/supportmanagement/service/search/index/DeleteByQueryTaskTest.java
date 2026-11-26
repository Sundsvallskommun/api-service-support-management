package se.sundsvall.supportmanagement.service.search.index;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
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

		final var task = task(Duration.ofMinutes(1));

		final var e = assertThrows(IllegalStateException.class, () -> task.run(query, _ -> {}));

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

		final var task = task(Duration.ofMinutes(1));

		final var e = assertThrows(IllegalStateException.class, () -> task.run(query, _ -> {}));

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

		final var task = task(Duration.ZERO);

		final var e = assertThrows(IllegalStateException.class, () -> task.run(query, _ -> {}));

		assertThat(e.getMessage()).isEqualTo("The removal of documents from the search index was not done within PT0S and was cancelled");
		assertThat(requests.getLast().getMethod()).isEqualTo("POST");
		assertThat(requests.getLast().getEndpoint()).isEqualTo("/_tasks/" + TASK_ID + "/_cancel");
	}

	@Test
	void aClusterThatDoesNotStartTheTaskFailsTheRemoval() throws IOException {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		clusterAnswers("{}");
		final var query = MAPPER.readTree("{}");

		final var task = task(Duration.ofMinutes(1));

		final var e = assertThrows(IllegalStateException.class, () -> task.run(query, _ -> {}));

		assertThat(e.getMessage()).isEqualTo(DeleteByQueryTask.NOT_STARTED);
		assertThat(requests).hasSize(1);
	}

	@Test
	void aClusterThatCannotBeReachedFailsTheRemoval() throws IOException {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		when(openSearchMock.restClient()).thenReturn(restClientMock);
		when(restClientMock.performRequest(any())).thenThrow(new IOException("Connection refused"));
		final var query = MAPPER.readTree("{}");

		final var task = task(Duration.ofMinutes(1));

		final var e = assertThrows(RuntimeException.class, () -> task.run(query, _ -> {}));

		assertThat(e.getMessage()).isEqualTo(DeleteByQueryTask.NOT_STARTED);
		assertThat(e.getCause()).hasMessage("Connection refused");
	}

	@Test
	void anErrorAnswerOfTheClusterFailsTheRemoval() throws IOException {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		when(openSearchMock.restClient()).thenReturn(restClientMock);
		final var response = mock(Response.class);
		when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 400, "Bad Request"));
		when(response.getRequestLine()).thenReturn(new BasicRequestLine("POST", "/errand-write/_delete_by_query", new ProtocolVersion("HTTP", 1, 1)));
		when(response.getHost()).thenReturn(new HttpHost("localhost", 9200));
		final var refused = new ResponseException(response);
		when(restClientMock.performRequest(any())).thenThrow(refused);
		final var query = MAPPER.readTree("{}");
		final var task = task(Duration.ofMinutes(1));

		final var e = assertThrows(IllegalStateException.class, () -> task.run(query, _ -> {}));

		assertThat(e.getMessage()).startsWith(DeleteByQueryTask.NOT_STARTED + ": ");
		assertThat(e.getCause()).isSameAs(refused);
	}

	@Test
	void aWaitThatIsInterruptedCancelsTheTaskAndKeepsTheInterrupt() throws IOException {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		clusterAnswers("{\"task\":\"" + TASK_ID + "\"}", "{}");
		final var query = MAPPER.readTree("{}");
		final var task = task(Duration.ofMinutes(1));

		Thread.currentThread().interrupt();
		try {
			final var e = assertThrows(IllegalStateException.class, () -> task.run(query, _ -> {}));

			assertThat(e.getMessage()).isEqualTo(DeleteByQueryTask.INTERRUPTED);
			assertThat(Thread.currentThread().isInterrupted()).isTrue();
			assertThat(requests.getLast().getEndpoint()).isEqualTo("/_tasks/" + TASK_ID + "/_cancel");
		} finally {
			Thread.interrupted();
		}
	}

	@Test
	void aTaskThatCannotBeCancelledOrForgottenIsLetBe() throws IOException {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		when(openSearchMock.restClient()).thenReturn(restClientMock);
		final var started = mock(Response.class);
		final var startedEntity = mock(HttpEntity.class);
		when(started.getEntity()).thenReturn(startedEntity);
		when(startedEntity.getContent()).thenReturn(new ByteArrayInputStream(("{\"task\":\"" + TASK_ID + "\"}").getBytes(UTF_8)));
		final var running = mock(Response.class);
		final var runningEntity = mock(HttpEntity.class);
		when(running.getEntity()).thenReturn(runningEntity);
		when(runningEntity.getContent()).thenReturn(new ByteArrayInputStream("{\"completed\":false}".getBytes(UTF_8)));
		when(restClientMock.performRequest(any())).thenReturn(started, running).thenThrow(new IOException("gone"));
		final var query = MAPPER.readTree("{}");
		final var task = task(Duration.ZERO);

		final var e = assertThrows(IllegalStateException.class, () -> task.run(query, _ -> {}));

		// The cancel that failed is logged, and what is reported is that the task was not done in time
		assertThat(e.getMessage()).isEqualTo(DeleteByQueryTask.NOT_DONE_IN_TIME.formatted(Duration.ZERO));
	}

	@Test
	void aResultThatCannotBeForgottenIsLetBe() throws IOException {
		when(openSearchMock.errandWriteIndex()).thenReturn("errand-write");
		when(openSearchMock.restClient()).thenReturn(restClientMock);
		final var started = mock(Response.class);
		final var startedEntity = mock(HttpEntity.class);
		when(started.getEntity()).thenReturn(startedEntity);
		when(startedEntity.getContent()).thenReturn(new ByteArrayInputStream(("{\"task\":\"" + TASK_ID + "\"}").getBytes(UTF_8)));
		final var done = mock(Response.class);
		final var doneEntity = mock(HttpEntity.class);
		when(done.getEntity()).thenReturn(doneEntity);
		when(doneEntity.getContent()).thenReturn(new ByteArrayInputStream("{\"completed\":true,\"response\":{\"deleted\":2}}".getBytes(UTF_8)));
		when(restClientMock.performRequest(any())).thenReturn(started, done).thenThrow(new IOException("system index"));

		assertThat(task(Duration.ofMinutes(1)).run(MAPPER.readTree("{}"), _ -> {})).isEqualTo(2);
	}
}

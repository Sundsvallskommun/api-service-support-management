package se.sundsvall.supportmanagement.service.search.index;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.config.SearchProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Removes the documents a query matches, as a task of the cluster rather than as one request.
 * <p>
 * A {@code _delete_by_query} answers when it is done, and over a large namespace on a busy cluster that is longer than
 * the client waits for an answer. Started as a task it answers at once with the id of the task, and the task is then
 * asked until it is done, each answer being reported to the caller so that a job carrying the removal is seen to be
 * alive. A task that is not done in time is cancelled, so that it does not go on removing beside whatever the caller
 * does next.
 */
@Component
public class DeleteByQueryTask {

	static final String NOT_STARTED = "The removal of documents from the search index could not be started";
	static final String NOT_DONE_IN_TIME = "The removal of documents from the search index was not done within %s and was cancelled";
	static final String FAILED = "The removal of documents from the search index failed: %s";
	static final String INTERRUPTED = "The removal of documents from the search index was interrupted and cancelled";

	private static final String ERROR = "error";
	private static final Logger LOG = LoggerFactory.getLogger(DeleteByQueryTask.class);
	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	private final OpenSearchClient openSearch;
	private final Duration timeout;
	private final Duration pollInterval;

	public DeleteByQueryTask(final OpenSearchClient openSearch, final SearchProperties properties) {
		this.openSearch = openSearch;
		this.timeout = properties.reindex().sweepTimeout();
		this.pollInterval = properties.reindex().pollInterval();
	}

	/**
	 * Removes the documents of the errand index the query matches, and waits for it.
	 *
	 * @param  query      the query, as the value of {@code query} in the body of a {@code _delete_by_query}
	 * @param  onProgress called with how many documents are removed so far, every time the task is asked
	 * @return            how many documents were removed
	 */
	public long run(final JsonNode query, final ProgressListener onProgress) {
		final var taskId = start(query);

		final JsonNode finished;
		try {
			finished = awaitCompletion(taskId, onProgress);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			cancel(taskId);
			throw new IllegalStateException(INTERRUPTED, e);
		} catch (final RuntimeException e) {
			// Whatever ends the wait - the deadline, a cluster that stopped answering, a progress that could not be
			// written - ends the removal too, so that it does not go on beside whatever the caller does next
			cancel(taskId);
			throw e;
		}

		forget(taskId);
		return outcomeOf(finished);
	}

	/** Asks the task until it is done, and answers with what it reported then. */
	private JsonNode awaitCompletion(final String taskId, final ProgressListener onProgress) throws InterruptedException {
		final var deadline = System.nanoTime() + timeout.toNanos();
		while (true) {
			Thread.sleep(pollInterval);

			final var task = get(taskId);
			onProgress.removed(task.path("task").path("status").path("deleted").asLong());

			if (task.path("completed").asBoolean()) {
				return task;
			}
			if (System.nanoTime() - deadline >= 0) {
				throw new IllegalStateException(NOT_DONE_IN_TIME.formatted(timeout));
			}
		}
	}

	private String start(final JsonNode query) {
		final var request = new Request("POST", "/" + openSearch.errandWriteIndex() + "/_delete_by_query");
		request.addParameter("conflicts", "proceed");
		request.addParameter("refresh", "true");
		request.addParameter("wait_for_completion", "false");

		final var body = MAPPER.createObjectNode();
		body.set("query", query);
		request.setJsonEntity(body.toString());

		final var taskId = perform(request, NOT_STARTED).path("task").asString();
		if (taskId.isBlank()) {
			throw new IllegalStateException(NOT_STARTED);
		}
		return taskId;
	}

	private JsonNode get(final String taskId) {
		return perform(new Request("GET", "/_tasks/" + taskId), FAILED.formatted("the task could not be asked how it is doing"));
	}

	/**
	 * What the task reports when it is done. A task that ran into documents it could not remove reports them as failures
	 * and still completes, so they are looked for rather than taken from the task having completed.
	 */
	private static long outcomeOf(final JsonNode task) {
		if (task.hasNonNull(ERROR)) {
			final var error = task.path(ERROR);
			throw new IllegalStateException(FAILED.formatted(error.path("reason").asString(error.toString())));
		}

		final var response = task.path("response");
		final var failures = response.path("failures");
		if (failures.isArray() && !failures.isEmpty()) {
			throw new IllegalStateException(FAILED.formatted("%d document(s) could not be removed, the first because: %s"
				.formatted(failures.size(), failures.get(0).path("cause").path("reason").asString(failures.get(0).toString()))));
		}

		return response.path("deleted").asLong();
	}

	/**
	 * The cluster keeps what a task started this way reported. Removing it is a courtesy and not part of the work, so a
	 * cluster that will not have it removed is let be.
	 */
	private void forget(final String taskId) {
		try {
			openSearch.restClient().performRequest(new Request("DELETE", "/.tasks/_doc/" + taskId));
		} catch (final IOException | RuntimeException e) {
			LOG.debug("The result of task {} could not be removed from the cluster: {}", taskId, e.getMessage());
		}
	}

	private void cancel(final String taskId) {
		try {
			openSearch.restClient().performRequest(new Request("POST", "/_tasks/" + taskId + "/_cancel"));
		} catch (final IOException | RuntimeException e) {
			LOG.warn("Task {} removing documents from the search index could not be cancelled and may still be running: {}", taskId, e.getMessage());
		}
	}

	private JsonNode perform(final Request request, final String failure) {
		try {
			final var response = openSearch.restClient().performRequest(request);
			try (var content = response.getEntity().getContent()) {
				return MAPPER.readTree(content);
			}
		} catch (final ResponseException e) {
			throw new IllegalStateException(failure + ": " + e.getMessage(), e);
		} catch (final IOException e) {
			throw new UncheckedIOException(failure, e);
		}
	}

	/** Told how far a removal has come. */
	@FunctionalInterface
	public interface ProgressListener {
		void removed(long documents);
	}
}

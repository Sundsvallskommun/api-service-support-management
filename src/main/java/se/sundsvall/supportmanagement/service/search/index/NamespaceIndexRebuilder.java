package se.sundsvall.supportmanagement.service.search.index;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import org.elasticsearch.client.Request;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEntityFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.model.ErrandIndex;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes the errands of one namespace into the search index anew, and then removes the documents of the namespace that
 * were not written, which are those of errands no longer in the database.
 * <p>
 * Written before anything is removed, rather than the other way around, so that the namespace is searched as it was
 * while the rebuild is under way rather than with a hole in it. Every document carries when it was written
 * ({@link ErrandIndex#INDEXED_AT}), and what the rebuild did not write is what was written before it started. A
 * document written while the rebuild is under way, by an errand changed meanwhile, is newer than that and stays. The
 * moment set apart is taken somewhat before the rebuild starts, so that a pod whose clock is a little behind does not
 * have what it writes meanwhile taken for something left over.
 * <p>
 * Should an errand not be written, nothing is removed: its document is the one there is, however old, and a rebuild
 * that went wrong part of the way is not a reason to leave the namespace with less than it had.
 */
@Component
public class NamespaceIndexRebuilder {

	static final Duration CLOCK_MARGIN = Duration.ofMinutes(1);
	static final int MAX_FAILURES_NAMED = 10;
	static final String NOT_ALL_WRITTEN = "%d errand(s) of the namespace could not be written to the search index, nothing was removed from it. Errand(s): %s";
	static final String FAILED = "The errands of the namespace could not be written to the search index: %s";

	private static final JsonMapper MAPPER = JsonMapper.builder().build();
	private static final String NAMESPACE_CONDITION = "e.namespace = :namespace and e.municipalityId = :municipalityId";

	private final EntityManagerFactory entityManagerFactory;
	private final OpenSearchClient openSearch;
	private final DeleteByQueryTask deleteByQuery;
	private final Clock clock;

	@Autowired
	public NamespaceIndexRebuilder(final EntityManagerFactory entityManagerFactory, final OpenSearchClient openSearch, final DeleteByQueryTask deleteByQuery) {
		this(entityManagerFactory, openSearch, deleteByQuery, Clock.systemUTC());
	}

	NamespaceIndexRebuilder(final EntityManagerFactory entityManagerFactory, final OpenSearchClient openSearch, final DeleteByQueryTask deleteByQuery, final Clock clock) {
		this.entityManagerFactory = entityManagerFactory;
		this.openSearch = openSearch;
		this.deleteByQuery = deleteByQuery;
		this.clock = clock;
	}

	/**
	 * Rebuilds the namespace and waits for it.
	 *
	 * @param  namespace      the namespace
	 * @param  municipalityId the municipality the namespace belongs to
	 * @param  progress       told how far the rebuild has come, as it goes
	 * @return                how many errands were written and how many documents removed
	 */
	public Outcome rebuild(final String namespace, final String municipalityId, final Progress progress) throws InterruptedException {
		final var writtenSince = clock.instant().minus(CLOCK_MARGIN);
		final var failures = new Failures();
		final var written = new AtomicLong();

		final var massIndexer = Search.mapping(entityManagerFactory).scope(ErrandEntity.class).massIndexer()
			// The purge that is on by default would empty the index of every namespace, not only the one being rebuilt
			.purgeAllOnStart(false)
			.monitor(new Monitor(written, progress))
			.failureHandler(failures);
		massIndexer.type(ErrandEntity.class).reindexOnly(NAMESPACE_CONDITION)
			.param("namespace", namespace)
			.param("municipalityId", municipalityId);

		try {
			massIndexer.startAndWait();
		} catch (final RuntimeException e) {
			// The mass indexer ends by throwing when an errand was not written, which the failures already say better
			if (failures.isEmpty()) {
				throw new IllegalStateException(FAILED.formatted(e.getMessage()), e);
			}
		}

		if (!failures.isEmpty()) {
			throw new IllegalStateException(failures.describe());
		}

		// What was written is only seen by the removal once the index has been refreshed
		refresh();

		final var removed = deleteByQuery.run(notWrittenSince(namespace, municipalityId, writtenSince.toEpochMilli()), progress::removed);
		return new Outcome(written.get(), removed);
	}

	private void refresh() {
		try {
			openSearch.restClient().performRequest(new Request("POST", "/" + openSearch.errandWriteIndex() + "/_refresh"));
		} catch (final IOException e) {
			throw new UncheckedIOException("The search index could not be refreshed", e);
		}
	}

	/**
	 * The documents of the namespace not written since the sent in moment, including those written before the moment of
	 * writing was kept at all. The namespace is indexed lowercased, and this goes to OpenSearch without passing the query
	 * DSL, so it is lowercased here.
	 */
	static JsonNode notWrittenSince(final String namespace, final String municipalityId, final long epochMillis) {
		final var query = MAPPER.createObjectNode();
		final var bool = query.putObject("bool");
		final var filter = bool.putArray("filter");
		filter.addObject().putObject("term").put(ErrandIndex.MUNICIPALITY_ID, municipalityId);
		filter.addObject().putObject("term").put(ErrandIndex.NAMESPACE, namespace.toLowerCase(Locale.ROOT));
		bool.putArray("must_not").addObject().putObject("range").putObject(ErrandIndex.INDEXED_AT)
			.put("gte", epochMillis)
			.put("format", "epoch_millis");
		return query;
	}

	/**
	 * @param written how many errands were written
	 * @param removed how many documents of errands no longer there were removed
	 */
	public record Outcome(long written, long removed) {
	}

	/** Told how far a rebuild has come. */
	public interface Progress {

		/** Called with how many errands are written so far. */
		void written(long errands);

		/** Called with how many documents are removed so far, once writing is done. */
		void removed(long documents);
	}

	private static final class Monitor implements MassIndexingMonitor {

		private final AtomicLong written;
		private final Progress progress;

		Monitor(final AtomicLong written, final Progress progress) {
			this.written = written;
			this.progress = progress;
		}

		@Override
		public void documentsAdded(final long increment) {
			progress.written(written.addAndGet(increment));
		}

		@Override
		public void documentsBuilt(final long increment) {
			// What is built is not yet written, and only what is written is reported
		}

		@Override
		public void entitiesLoaded(final long increment) {
			// As above
		}

		@Override
		public void indexingCompleted() {
			// The caller is told when the whole rebuild is done
		}
	}

	/**
	 * What the mass indexer could not do. An errand it could not write is named, up to {@link #MAX_FAILURES_NAMED} of
	 * them, so that the data keeping it out of the index can be looked at.
	 */
	static final class Failures implements MassIndexingFailureHandler {

		private final List<String> errandIds = new ArrayList<>();
		private final List<String> other = new ArrayList<>();
		private long count;

		@Override
		public synchronized void handle(final MassIndexingFailureContext context) {
			count++;
			if (other.size() < MAX_FAILURES_NAMED) {
				other.add(String.valueOf(context.throwable().getMessage()));
			}
		}

		@Override
		public synchronized void handle(final MassIndexingEntityFailureContext context) {
			final var references = context.failingEntityReferences();
			if (references.isEmpty()) {
				handle((MassIndexingFailureContext) context);
				return;
			}
			references.forEach(reference -> {
				count++;
				if (errandIds.size() < MAX_FAILURES_NAMED) {
					errandIds.add(String.valueOf(reference.id()));
				}
			});
		}

		synchronized boolean isEmpty() {
			return count == 0;
		}

		synchronized String describe() {
			final var named = new ArrayList<>(errandIds);
			if (named.isEmpty()) {
				named.addAll(other);
			}
			return NOT_ALL_WRITTEN.formatted(count, String.join(", ", named) + (count > named.size() ? " and %d more".formatted(count - named.size()) : ""));
		}
	}
}

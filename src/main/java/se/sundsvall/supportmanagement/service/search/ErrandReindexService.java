package se.sundsvall.supportmanagement.service.search;

import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.time.Instant;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.config.SearchProperties;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * Rebuilds the search index from the database.
 * <p>
 * The index is disposable: the database is the source of truth, and a document that was not written because OpenSearch
 * was unreachable at the time, or that predates a change to the mapping, is put right by a rebuild. One rebuild runs at
 * a time across every pod of the service, guarded by the same lock table the schedulers use, since two mass indexers
 * on the same index only slow each other down.
 */
@Service
public class ErrandReindexService {

	static final String LOCK_NAME = "errand-reindex";
	static final String REINDEX_RUNNING = "A reindex is already running";

	private static final Logger LOG = LoggerFactory.getLogger(ErrandReindexService.class);
	private static final String NAMESPACE_CONDITION = "e.namespace = :namespace and e.municipalityId = :municipalityId";

	private final EntityManagerFactory entityManagerFactory;
	private final LockProvider lockProvider;
	private final SearchAvailability availability;
	private final SearchProperties properties;

	public ErrandReindexService(final EntityManagerFactory entityManagerFactory, final LockProvider lockProvider, final SearchAvailability availability, final SearchProperties properties) {
		this.entityManagerFactory = entityManagerFactory;
		this.lockProvider = lockProvider;
		this.availability = availability;
		this.properties = properties;
	}

	/**
	 * Starts a rebuild and returns at once, the rebuild goes on in the background and reports in the log.
	 * <p>
	 * A rebuild of a namespace writes its errands over whatever the index holds for them and leaves the rest of the index
	 * as it is, so it can run while the service is in use. A full rebuild drops the index and creates it anew from the
	 * mapping, which is what a changed mapping calls for, and empties the search of every namespace until it is done.
	 *
	 * @param  namespace                                      namespace to rebuild
	 * @param  municipalityId                                 municipality id
	 * @param  full                                           true to rebuild the whole index across every namespace,
	 *                                                        recreating the schema
	 * @throws org.springframework.web.ErrorResponseException 409 when a rebuild is already running, 503 when the
	 *                                                        environment has no search index
	 */
	public void reindex(final String namespace, final String municipalityId, final boolean full) {
		availability.verifyEnabled();

		final var lock = lockProvider.lock(new LockConfiguration(Instant.now(), LOCK_NAME, properties.reindex().lockAtMostFor(), Duration.ZERO))
			.orElseThrow(() -> Problem.valueOf(CONFLICT, REINDEX_RUNNING));

		final var description = full ? "the whole errand index" : "errands of namespace '%s' for municipality '%s'".formatted(namespace, municipalityId);
		LOG.info("Reindex of {} starting", description);

		try {
			massIndexer(namespace, municipalityId, full)
				.start()
				.whenComplete((_, throwable) -> finish(lock, description, throwable));
		} catch (final RuntimeException e) {
			lock.unlock();
			throw e;
		}
	}

	private MassIndexer massIndexer(final String namespace, final String municipalityId, final boolean full) {
		final var massIndexer = Search.mapping(entityManagerFactory).scope(ErrandEntity.class).massIndexer();

		if (full) {
			return massIndexer.dropAndCreateSchemaOnStart(true);
		}

		massIndexer.type(ErrandEntity.class).reindexOnly(NAMESPACE_CONDITION)
			.param("namespace", namespace)
			.param("municipalityId", municipalityId);

		// The purge that is on by default would empty the index of every namespace, not only the one being rebuilt
		return massIndexer.purgeAllOnStart(false);
	}

	private static void finish(final SimpleLock lock, final String description, final Throwable throwable) {
		try {
			if (throwable == null) {
				LOG.info("Reindex of {} finished", description);
			} else {
				LOG.error("Reindex of {} failed", description, throwable);
			}
		} finally {
			lock.unlock();
		}
	}
}

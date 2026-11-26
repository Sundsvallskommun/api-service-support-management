package se.sundsvall.supportmanagement.service.search.index;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.hibernate.search.mapper.orm.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.config.JobProperties;
import se.sundsvall.supportmanagement.config.SearchProperties;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.job.JobService;
import se.sundsvall.supportmanagement.service.job.JobSpec;
import se.sundsvall.supportmanagement.service.job.SearchReindexRun;
import se.sundsvall.supportmanagement.service.job.SearchReindexRunner;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.springframework.http.HttpStatus.CONFLICT;
import static se.sundsvall.supportmanagement.model.enums.JobStatus.COMPLETED;
import static se.sundsvall.supportmanagement.model.enums.JobType.SEARCH_REINDEX;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

/**
 * Rebuilds the search index from the database.
 * <p>
 * The index is disposable: the database is the source of truth, and a document that was not written because OpenSearch
 * was unreachable at the time, or that predates a change to the mapping, is put right by a rebuild. One rebuild runs at
 * a time across every pod of the service, guarded by the same lock table the schedulers use, since two mass indexers
 * on the same index only slow each other down.
 * <p>
 * A rebuild of a namespace is carried out as a job, whether asked for or nightly, and reports against it. It writes the
 * errands of the namespace anew before it removes what they did not overwrite, so the namespace is searched as it was
 * while it is under way (see {@link NamespaceIndexRebuilder}). A rebuild of the whole index belongs to no namespace and
 * so to no job.
 */
@Service
public class ErrandReindexService {

	static final String LOCK_NAME = "errand-reindex";
	static final String REINDEX_RUNNING = "A reindex is already running";
	static final String COULD_NOT_START = "Search reindex could not be started: %s";
	static final String NOT_COMPLETED = "ended as %s: %s";

	private static final Logger LOG = LoggerFactory.getLogger(ErrandReindexService.class);

	/** What the nightly reindex carries out its jobs on, one after the other on its own thread. */
	private static final AsyncTaskExecutor ON_THIS_THREAD = new TaskExecutorAdapter(Runnable::run);

	private final EntityManagerFactory entityManagerFactory;
	private final LockProvider lockProvider;
	private final SearchAvailability availability;
	private final SearchProperties properties;
	private final AccessControlService accessControlService;
	private final NamespaceConfigRepository namespaceConfigRepository;
	private final ErrandsRepository errandsRepository;
	private final JobService jobService;
	private final SearchReindexRunner runner;
	private final AsyncTaskExecutor taskExecutor;
	private final SearchIndexHealth indexHealth;
	private final OpenSearchClient openSearch;
	private final Duration jobStaleAfter;

	public ErrandReindexService(final EntityManagerFactory entityManagerFactory, final LockProvider lockProvider, final SearchAvailability availability,
		final SearchProperties properties, final AccessControlService accessControlService, final NamespaceConfigRepository namespaceConfigRepository,
		final ErrandsRepository errandsRepository, final JobService jobService, final SearchReindexRunner runner,
		@Qualifier("searchReindexTaskExecutor") final AsyncTaskExecutor taskExecutor, final SearchIndexHealth indexHealth, final OpenSearchClient openSearch,
		final JobProperties jobProperties) {

		this.entityManagerFactory = entityManagerFactory;
		this.lockProvider = lockProvider;
		this.availability = availability;
		this.properties = properties;
		this.accessControlService = accessControlService;
		this.namespaceConfigRepository = namespaceConfigRepository;
		this.errandsRepository = errandsRepository;
		this.jobService = jobService;
		this.runner = runner;
		this.taskExecutor = taskExecutor;
		this.indexHealth = indexHealth;
		this.openSearch = openSearch;
		this.jobStaleAfter = jobProperties.staleAfter();
	}

	/**
	 * Rebuilds every namespace there is, one after the other, each as a job of its own, and waits for them.
	 * <p>
	 * What the index misses it misses quietly: indexing follows a commit without holding the request up, so a write that
	 * did not reach OpenSearch is a document left as it was, or an errand deleted that is still found. Nothing says which
	 * errands those are, so the way back to an index that agrees with the database is to write it again, which is what
	 * this does nightly. One namespace at a time, so that the cluster is asked for no more than one rebuild at once.
	 * <p>
	 * The index is created first where it is missing, as it is after OpenSearch restarted without its data while this
	 * instance went on running: the schema is otherwise only put in place when the service starts, and a rebuild of a
	 * namespace writes into the index without creating it. An index that is there is left as it is.
	 * <p>
	 * A namespace whose rebuild fails is passed, so that it does not leave every namespace after it unrepaired night
	 * after night, and what failed is raised at the end so that the scheduled job reports it.
	 * <p>
	 * Skipped where the environment has no search index, and where a rebuild is already running: the one under way is
	 * doing this very work, and two mass indexers on one index only slow each other down.
	 */
	public void reindexEveryNamespace() {
		if (!availability.isEnabled()) {
			LOG.info("Search is not enabled, skipping the nightly reindex");
			return;
		}

		final var lock = lock();
		if (lock == null) {
			LOG.info("{}, skipping the nightly reindex", REINDEX_RUNNING);
			return;
		}

		try {
			createIndexIfMissing();

			final var namespaces = namespaceConfigRepository.findAll();
			final var failed = new ArrayList<String>();

			for (final var config : namespaces) {
				final var outcome = reindexAndWait(config.getNamespace(), config.getMunicipalityId());
				if (outcome != null) {
					failed.add("%s/%s (%s)".formatted(config.getMunicipalityId(), config.getNamespace(), outcome));
				}
			}

			if (!failed.isEmpty()) {
				throw new IllegalStateException("Nightly reindex rebuilt %d of %d namespaces, these failed: %s"
					.formatted(namespaces.size() - failed.size(), namespaces.size(), String.join(", ", failed)));
			}

			LOG.info("Nightly reindex finished, {} namespaces rebuilt", namespaces.size());
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Rebuilds one namespace as a job carried out on this thread, and says how it went.
	 *
	 * @return why the rebuild did not complete, or {@code null} when it did
	 */
	private String reindexAndWait(final String namespace, final String municipalityId) {
		try {
			final var job = launch(namespace, municipalityId, ON_THIS_THREAD, runner::run);
			if (job.getStatus() == COMPLETED) {
				return null;
			}
			LOG.error("Reindex of errands of namespace '{}' for municipality '{}' failed, going on with the rest: {}", namespace, municipalityId, job.getMessage());
			return NOT_COMPLETED.formatted(job.getStatus(), job.getMessage());
		} catch (final RuntimeException e) {
			LOG.error("Reindex of errands of namespace '{}' for municipality '{}' could not be started, going on with the rest", namespace, municipalityId, e);
			return e.getMessage();
		}
	}

	/**
	 * Starts a rebuild of a namespace as a job and returns at once, the rebuild goes on in the background and is followed
	 * through the job.
	 * <p>
	 * The errands of the namespace are written anew and what they did not overwrite is removed after, which leaves the
	 * rest of the index as it is and the namespace searchable while it is under way. An errand changed while the rebuild
	 * is passing over it may be written back as it was before the change, which its next change puts right.
	 * <p>
	 * Held to the same grant as the configuration of the namespace, since a rebuild is an operation on the namespace
	 * rather than on any errand.
	 *
	 * @param  namespace                                      namespace to rebuild
	 * @param  municipalityId                                 municipality id
	 * @return                                                the job the rebuild reports against
	 * @throws org.springframework.web.ErrorResponseException 403 when the user may not administer the namespace, 409
	 *                                                        when a rebuild is already running, 503 when the environment
	 *                                                        has no search index
	 */
	public JobResponse reindex(final String namespace, final String municipalityId) {
		availability.verifyEnabled();
		accessControlService.verifyNamespaceAuthorization(namespace, municipalityId, ProtectedResource.NAMESPACE_CONFIG, RW);

		final var lock = lock();
		if (lock == null) {
			throw Problem.valueOf(CONFLICT, REINDEX_RUNNING);
		}

		// Released once, by the run when it ends or here when it never started
		final var unlock = new Unlock(lock);
		try {
			return launch(namespace, municipalityId, taskExecutor, run -> {
				try {
					runner.run(run);
				} finally {
					unlock.run();
				}
			});
		} catch (final RuntimeException e) {
			unlock.run();
			throw e;
		}
	}

	private JobResponse launch(final String namespace, final String municipalityId, final AsyncTaskExecutor executor, final Consumer<SearchReindexRun> carryOut) {
		// A rebuild whose instance went away mid run would otherwise keep the namespace from being rebuilt until the job is
		// swept as abandoned. One that is still alive cannot be found here, since it holds the lock this is called under
		if (!jobService.stealStaleLease(namespace, municipalityId, SEARCH_REINDEX, jobStaleAfter)) {
			throw Problem.valueOf(CONFLICT, REINDEX_RUNNING);
		}

		final var total = (int) Math.min(Integer.MAX_VALUE, errandsRepository.count(withNamespace(namespace).and(withMunicipalityId(municipalityId))));

		return jobService.launch(new JobSpec(namespace, municipalityId, SEARCH_REINDEX, total, null),
			executor,
			jobId -> new SearchReindexRun(jobId, namespace, municipalityId),
			carryOut,
			COULD_NOT_START);
	}

	/**
	 * Drops the whole index and builds it again from the database, which is what a changed mapping calls for.
	 * <p>
	 * An operation on no namespace in particular: the index holds every namespace of every municipality, and every one of
	 * them searches nothing until the rebuild is over. So it is not guarded as the namespace in a path would be - there is
	 * no such namespace - but by every namespace that enforces access control at all: the caller administers all of them,
	 * or they administer none of this. A cluster where no namespace enforces anything leaves it open, as everything else
	 * is open there.
	 * <p>
	 * The index is created anew of the layout the service writes, so whatever the health of the service said the index
	 * wanted is no longer wanted once this is done.
	 *
	 * @throws org.springframework.web.ErrorResponseException 403 where a namespace that enforces access control is not
	 *                                                        the caller's to administer, 409 when a rebuild is already
	 *                                                        running, 503 where the environment has no search index
	 */
	public void reindexEverything() {
		availability.verifyEnabled();
		namespaceConfigRepository.findAll()
			.forEach(config -> accessControlService.verifyNamespaceAuthorization(config.getNamespace(), config.getMunicipalityId(), ProtectedResource.NAMESPACE_CONFIG, RW));

		final var lock = lock();
		if (lock == null) {
			throw Problem.valueOf(CONFLICT, REINDEX_RUNNING);
		}

		LOG.info("Reindex of the whole errand index starting");

		try {
			Search.mapping(entityManagerFactory).scope(ErrandEntity.class).massIndexer()
				.dropAndCreateSchemaOnStart(true)
				.start()
				.whenComplete((_, throwable) -> {
					try {
						if (throwable == null) {
							indexHealth.created();
							LOG.info("Reindex of the whole errand index finished");
						} else {
							LOG.error("Reindex of the whole errand index failed", throwable);
						}
					} finally {
						lock.unlock();
					}
				});
		} catch (final RuntimeException e) {
			lock.unlock();
			throw e;
		}
	}

	private void createIndexIfMissing() {
		try {
			if (!openSearch.errandIndexExists()) {
				Search.mapping(entityManagerFactory).scope(ErrandEntity.class).schemaManager().createIfMissing();
				indexHealth.created();
			}
		} catch (final IOException e) {
			throw new IllegalStateException("Whether the search index is there could not be found out", e);
		}
	}

	private SimpleLock lock() {
		return lockProvider.lock(new LockConfiguration(Instant.now(), LOCK_NAME, properties.reindex().lockAtMostFor(), Duration.ZERO)).orElse(null);
	}

	/** Releases a lock the first time it is run, and does nothing after. */
	private static final class Unlock implements Runnable {

		private final SimpleLock lock;
		private final AtomicBoolean released = new AtomicBoolean();

		Unlock(final SimpleLock lock) {
			this.lock = lock;
		}

		@Override
		public void run() {
			if (released.compareAndSet(false, true)) {
				lock.unlock();
			}
		}
	}
}

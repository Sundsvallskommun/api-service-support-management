package se.sundsvall.supportmanagement.service.job;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.service.search.index.NamespaceIndexRebuilder;
import se.sundsvall.supportmanagement.service.search.index.SearchIndexHealth;

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Carries out rebuilds of the search index of a namespace, each against a job of its own.
 * <p>
 * The errands of the namespace are written first and what they did not overwrite is removed after, see
 * {@link NamespaceIndexRebuilder}. The job is reported on as the errands are written and while the removal is under
 * way, which is what keeps a rebuild waiting on the search cluster from being taken for one whose instance is gone.
 * <p>
 * Holds no lock of its own: one rebuild at a time is the business of whoever starts it.
 */
@Component
public class SearchReindexRunner extends JobRunner<SearchReindexRun> {

	static final Duration REPORT_INTERVAL = Duration.ofSeconds(5);
	static final String WRITING = "Writing the errands of the namespace to the search index";
	static final String REMOVING = "Removing the documents of errands no longer there, %d so far";
	static final String SUMMARY = "%d errand(s) written, %d document(s) of errands no longer there removed";
	static final String ENDED_WITHOUT_RESULT = "Search reindex ended without reaching a result of its own";
	static final String INTERRUPTED = "Search reindex was interrupted";

	private static final Logger LOG = LoggerFactory.getLogger(SearchReindexRunner.class);

	private final JobService jobService;
	private final NamespaceIndexRebuilder rebuilder;
	private final SearchIndexHealth indexHealth;
	private final Duration reportInterval;

	@Autowired
	SearchReindexRunner(final JobService jobService, final NamespaceIndexRebuilder rebuilder, final SearchIndexHealth indexHealth) {
		this(jobService, rebuilder, indexHealth, REPORT_INTERVAL);
	}

	SearchReindexRunner(final JobService jobService, final NamespaceIndexRebuilder rebuilder, final SearchIndexHealth indexHealth, final Duration reportInterval) {
		super(jobService);
		this.jobService = jobService;
		this.rebuilder = rebuilder;
		this.indexHealth = indexHealth;
		this.reportInterval = reportInterval;
	}

	@Override
	protected String jobId(final SearchReindexRun run) {
		return run.jobId();
	}

	@Override
	protected void work(final SearchReindexRun run) {
		LOG.info("Search reindex {} of namespace {} in municipality {} started", run.jobId(), sanitizeForLogging(run.namespace()), sanitizeForLogging(run.municipalityId()));

		final var reporter = new Reporter(run.jobId());
		final NamespaceIndexRebuilder.Outcome outcome;
		try {
			outcome = rebuilder.rebuild(run.namespace(), run.municipalityId(), reporter);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(INTERRUPTED, e);
		}

		jobService.complete(run.jobId(), SUMMARY.formatted(outcome.written(), outcome.removed()));
		LOG.info("Search reindex {} of namespace {} in municipality {} ended: {}", run.jobId(), sanitizeForLogging(run.namespace()), sanitizeForLogging(run.municipalityId()),
			SUMMARY.formatted(outcome.written(), outcome.removed()));

		// The rebuild may have brought fields the index did not hold before
		indexHealth.inspect();
	}

	@Override
	protected String reportAborted(final SearchReindexRun run, final Exception e) {
		LOG.error("Search reindex {} of namespace {} in municipality {} failed", run.jobId(), sanitizeForLogging(run.namespace()), sanitizeForLogging(run.municipalityId()), e);
		return e.getMessage();
	}

	@Override
	protected String endedWithoutResultMessage() {
		return ENDED_WITHOUT_RESULT;
	}

	/**
	 * Reports to the job at most every report interval ({@link #REPORT_INTERVAL} unless told otherwise) while errands are
	 * written, the mass indexer telling of
	 * every batch, and every time the removal is asked about, which is seldom enough by itself.
	 */
	private final class Reporter implements NamespaceIndexRebuilder.Progress {

		private final String jobId;
		private long lastReport;
		private int written;

		Reporter(final String jobId) {
			this.jobId = jobId;
			this.lastReport = System.nanoTime();
		}

		@Override
		public synchronized void written(final long errands) {
			written = (int) Math.min(Integer.MAX_VALUE, errands);
			if (System.nanoTime() - lastReport >= reportInterval.toNanos()) {
				jobService.reportProgress(jobId, written, WRITING);
				lastReport = System.nanoTime();
			}
		}

		@Override
		public synchronized void removed(final long documents) {
			jobService.reportProgress(jobId, written, REMOVING.formatted(documents));
			lastReport = System.nanoTime();
		}
	}
}

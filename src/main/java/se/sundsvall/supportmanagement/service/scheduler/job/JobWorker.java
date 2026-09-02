package se.sundsvall.supportmanagement.service.scheduler.job;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.config.JobProperties;
import se.sundsvall.supportmanagement.integration.db.model.JobEntity;
import se.sundsvall.supportmanagement.service.JobService;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

/**
 * Ends the jobs nobody is reporting on any more.
 * <p>
 * A run taken down with the instance carrying it out leaves a job that says it is running and nothing that will ever
 * say otherwise. The row would read that way for as long as it lives, and since a job under way rules out another run
 * of its kind in the same namespace, every run after it would be refused. Nothing else moves such a job, which is what
 * this is here for.
 * <p>
 * Jobs are not removed, however old. What a run leaves behind is one row, and for a purge that row is the only lasting
 * record that the errands it walked were removed on purpose.
 */
@Component
public class JobWorker {

	/**
	 * How many jobs the account of a sweep names one by one. The rest are counted, since what is read here is a line in
	 * a health page rather than a report: enough to see what kind of work stopped and where to go looking, without the
	 * page turning into a list.
	 */
	private static final int MAX_JOBS_NAMED = 5;

	private static final String ABANDONED = "%d run(s) stopped being reported on and were ended here, leaving the work each was carrying out half done: %s.";
	private static final String AND_MORE = " and %d more";
	private static final String JOB = "%s %s in namespace %s for municipality %s, last written to %s";

	private final JobService jobService;
	private final JobProperties properties;

	public JobWorker(final JobService jobService, final JobProperties properties) {
		this.jobService = jobService;
		this.properties = properties;
	}

	/**
	 * Ends the jobs that stopped being reported on, and accounts for what was ended.
	 * <p>
	 * The account is what the caller reports the health of the service against, so it names the work rather than
	 * counting it: whoever reads a service that has gone restricted needs to know which kind of run stopped, in which
	 * namespace, and when it was last heard from - not that something, somewhere, went wrong.
	 *
	 * @return what was ended, in a sentence, or empty when nothing had been abandoned.
	 */
	public Optional<String> endAbandonedJobs() {
		final var abandoned = jobService.failStaleJobs(properties.staleAfter());

		return abandoned.isEmpty() ? Optional.empty() : Optional.of(accountOf(abandoned));
	}

	private static String accountOf(final List<JobEntity> abandoned) {
		final var named = abandoned.stream()
			.limit(MAX_JOBS_NAMED)
			.map(JobWorker::accountOf)
			.collect(joining("; "));

		final var unnamed = abandoned.size() - Math.min(abandoned.size(), MAX_JOBS_NAMED);

		return ABANDONED.formatted(abandoned.size(), named + (unnamed > 0 ? AND_MORE.formatted(unnamed) : ""));
	}

	private static String accountOf(final JobEntity job) {
		return JOB.formatted(job.getType(), job.getId(), job.getNamespace(), job.getMunicipalityId(),
			ofNullable(job.getModified()).orElse(job.getCreated()));
	}
}

package se.sundsvall.supportmanagement.service.scheduler.job;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

@Service
public class JobScheduler {

	private final JobWorker jobWorker;
	private final Dept44HealthUtility healthUtility;

	@Value("${scheduler.job.name}")
	private String jobName;

	public JobScheduler(final JobWorker jobWorker, final Dept44HealthUtility healthUtility) {
		this.jobWorker = jobWorker;
		this.healthUtility = healthUtility;
	}

	@Dept44Scheduled(
		cron = "${scheduler.job.cron}",
		name = "${scheduler.job.name}",
		lockAtMostFor = "${scheduler.job.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.job.maximum-execution-time}")
	public void maintainJobs() {
		// A run that never reached an end of its own is work that quietly stopped halfway, and the only trace of it is
		// the job it left behind. Reported as restricted rather than only logged, so that it is answered for where the
		// state of the service is read - and reported with the account of what was ended, since that is what turns a
		// service marked restricted in Spring Boot Admin into something a reader can act on: which kind of run stopped,
		// in which namespace, and when it was last heard from.
		//
		// The indicator is left to the scheduling aspect from here. It clears the errors of a run before the next one
		// starts, so this holds until a sweep finds nothing abandoned, and no longer.
		jobWorker.endAbandonedJobs()
			.ifPresent(account -> healthUtility.setHealthIndicatorUnhealthy(jobName, account));
	}
}

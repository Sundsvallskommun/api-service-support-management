package se.sundsvall.supportmanagement.service.scheduler.softdeletederrands;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

/**
 * Deletes for good the errands that have been soft deleted for longer than their namespace keeps them. How long that is
 * is set per namespace, and a namespace that sets nothing keeps its soft deleted errands.
 */
@Service
public class SoftDeletedErrandScheduler {

	private final SoftDeletedErrandWorker softDeletedErrandWorker;
	private final Dept44HealthUtility healthUtility;
	private final String jobName;

	public SoftDeletedErrandScheduler(final SoftDeletedErrandWorker softDeletedErrandWorker, final Dept44HealthUtility healthUtility,
		@Value("${scheduler.soft-deleted-errands.name}") final String jobName) {
		this.softDeletedErrandWorker = softDeletedErrandWorker;
		this.healthUtility = healthUtility;
		this.jobName = jobName;
	}

	@Dept44Scheduled(
		cron = "${scheduler.soft-deleted-errands.cron}",
		name = "${scheduler.soft-deleted-errands.name}",
		lockAtMostFor = "${scheduler.soft-deleted-errands.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.soft-deleted-errands.maximum-execution-time}")
	void deleteSoftDeletedErrands() {
		// A namespace whose walk was aborted keeps its soft deleted errands until a later run gets through, so it is
		// reported where the state of the service is read rather than only logged. The scheduling aspect clears it
		// before the next run.
		softDeletedErrandWorker.deleteSoftDeletedErrands()
			.ifPresent(account -> healthUtility.setHealthIndicatorUnhealthy(jobName, account));
	}
}

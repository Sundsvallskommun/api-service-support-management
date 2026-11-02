package se.sundsvall.supportmanagement.service.scheduler.softdeletederrands;

import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

/**
 * Deletes for good the errands that have been soft deleted for longer than their namespace keeps them. How long that is
 * is set per namespace, and a namespace that sets nothing keeps its soft deleted errands.
 */
@Service
public class SoftDeletedErrandScheduler {

	private final SoftDeletedErrandWorker softDeletedErrandWorker;

	public SoftDeletedErrandScheduler(final SoftDeletedErrandWorker softDeletedErrandWorker) {
		this.softDeletedErrandWorker = softDeletedErrandWorker;
	}

	@Dept44Scheduled(
		cron = "${scheduler.soft-deleted-errands.cron}",
		name = "${scheduler.soft-deleted-errands.name}",
		lockAtMostFor = "${scheduler.soft-deleted-errands.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.soft-deleted-errands.maximum-execution-time}")
	void deleteSoftDeletedErrands() {
		softDeletedErrandWorker.deleteSoftDeletedErrands();
	}
}

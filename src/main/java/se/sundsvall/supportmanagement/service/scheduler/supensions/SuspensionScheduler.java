package se.sundsvall.supportmanagement.service.scheduler.supensions;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

@Service
@Transactional
public class SuspensionScheduler {

	private final SuspensionWorker suspensionWorker;

	public SuspensionScheduler(final SuspensionWorker suspensionWorker) {
		this.suspensionWorker = suspensionWorker;
	}

	@Dept44Scheduled(
		cron = "${scheduler.suspension.cron}",
		name = "${scheduler.suspension.name}",
		lockAtMostFor = "${scheduler.suspension.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.suspension.maximum-execution-time}")
	void processExpiredSuspensions() {
		suspensionWorker.processExpiredSuspensions();
	}
}

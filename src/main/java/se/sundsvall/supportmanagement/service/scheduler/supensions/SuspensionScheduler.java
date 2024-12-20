package se.sundsvall.supportmanagement.service.scheduler.supensions;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.requestid.RequestId;

@Service
@Transactional
public class SuspensionScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(SuspensionScheduler.class);

	private final SuspensionWorker suspensionWorker;

	public SuspensionScheduler(final SuspensionWorker suspensionWorker) {
		this.suspensionWorker = suspensionWorker;
	}

	@Scheduled(cron = "${scheduler.suspension.cron}")
	@SchedulerLock(name = "clean_suspensions", lockAtMostFor = "${scheduler.suspension.shedlock-lock-at-most-for}")
	void processExpiredSuspensions() {
		try {
			RequestId.init();

			LOG.debug("Processing suspensions");
			suspensionWorker.processExpiredSuspensions();
			LOG.debug("Finished processing suspensions");
		} finally {
			RequestId.reset();
		}
	}
}

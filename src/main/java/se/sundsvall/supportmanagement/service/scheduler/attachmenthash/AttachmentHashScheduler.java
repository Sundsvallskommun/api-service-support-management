package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

@Service
public class AttachmentHashScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(AttachmentHashScheduler.class);

	private final AttachmentHashWorker attachmentHashWorker;
	private final boolean enabled;

	public AttachmentHashScheduler(final AttachmentHashWorker attachmentHashWorker,
		@Value("${scheduler.attachment-hash.enabled:false}") final boolean enabled) {
		this.attachmentHashWorker = attachmentHashWorker;
		this.enabled = enabled;
	}

	@Dept44Scheduled(
		cron = "${scheduler.attachment-hash.cron}",
		name = "${scheduler.attachment-hash.name}",
		lockAtMostFor = "${scheduler.attachment-hash.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.attachment-hash.maximum-execution-time}")
	void computeAttachmentHashes() {
		if (!enabled) {
			LOG.info("scheduler.attachment-hash.enabled=false, skipping scheduled execution");
			return;
		}
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();
	}
}

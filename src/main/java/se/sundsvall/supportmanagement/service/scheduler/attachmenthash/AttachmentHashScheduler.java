package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

@Service
public class AttachmentHashScheduler {

	private final AttachmentHashWorker attachmentHashWorker;

	public AttachmentHashScheduler(final AttachmentHashWorker attachmentHashWorker) {
		this.attachmentHashWorker = attachmentHashWorker;
	}

	@Dept44Scheduled(
		cron = "${scheduler.attachment-hash.cron}",
		name = "${scheduler.attachment-hash.name}",
		lockAtMostFor = "${scheduler.attachment-hash.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.attachment-hash.maximum-execution-time}")
	void computeAttachmentHashes() {
		attachmentHashWorker.computeHashForAttachmentsWithoutHash();
	}
}

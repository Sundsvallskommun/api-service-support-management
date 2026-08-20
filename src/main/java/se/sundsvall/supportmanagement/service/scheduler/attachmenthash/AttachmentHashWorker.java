package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;

@Component
public class AttachmentHashWorker {

	private static final Logger LOG = LoggerFactory.getLogger(AttachmentHashWorker.class);

	private final AttachmentRepository attachmentRepository;
	private final AttachmentHashBatchProcessor batchProcessor;
	private final int batchSize;

	public AttachmentHashWorker(final AttachmentRepository attachmentRepository, final AttachmentHashBatchProcessor batchProcessor,
		@Value("${scheduler.attachment-hash.batch-size:250}") final int batchSize) {
		this.attachmentRepository = attachmentRepository;
		this.batchProcessor = batchProcessor;
		this.batchSize = batchSize;
	}

	public void computeHashForAttachmentsWithoutHash() {
		final var attachmentIds = attachmentRepository.findIdsByHashIsNull(Pageable.ofSize(batchSize));

		if (attachmentIds.isEmpty()) {
			LOG.info("No attachments without hash found");
			return;
		}

		LOG.info("Found {} attachments without hash, starting hash computation", attachmentIds.size());

		var totalProcessed = 0;
		var totalFailed = 0;

		for (final var attachmentId : attachmentIds) {
			try {
				if (batchProcessor.processAttachment(attachmentId)) {
					totalProcessed++;
				} else {
					totalFailed++;
				}
			} catch (final Exception e) {
				totalFailed++;
				LOG.warn("Failed to process attachment with id: {}", attachmentId, e);
			}
		}

		LOG.info("Hash computation completed. Processed {} attachments successfully, {} failed", totalProcessed, totalFailed);
	}
}

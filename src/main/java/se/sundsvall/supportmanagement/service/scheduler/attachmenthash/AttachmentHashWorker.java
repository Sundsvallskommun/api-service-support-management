package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;

@Component
public class AttachmentHashWorker {

	private static final int PAGE_SIZE = 100;
	private static final Logger LOG = LoggerFactory.getLogger(AttachmentHashWorker.class);

	private final AttachmentRepository attachmentRepository;
	private final AttachmentHashBatchProcessor batchProcessor;

	public AttachmentHashWorker(final AttachmentRepository attachmentRepository, final AttachmentHashBatchProcessor batchProcessor) {
		this.attachmentRepository = attachmentRepository;
		this.batchProcessor = batchProcessor;
	}

	public void computeHashForAttachmentsWithoutHash() {
		var totalProcessed = 0;
		var page = attachmentRepository.findByHashIsNull(PageRequest.of(0, PAGE_SIZE));

		if (page.isEmpty()) {
			LOG.info("No attachments without hash found");
			return;
		}

		final var totalElements = page.getTotalElements();
		final var maxIterations = (totalElements + PAGE_SIZE - 1) / PAGE_SIZE;
		var iteration = 0;

		LOG.info("Found {} attachments without hash, starting hash computation", totalElements);

		while (!page.isEmpty()) {
			final var ids = page.getContent().stream()
				.map(AttachmentEntity::getId)
				.toList();

			totalProcessed += batchProcessor.processBatch(ids);
			iteration++;

			if (iteration >= maxIterations) {
				break;
			}

			page = attachmentRepository.findByHashIsNull(PageRequest.of(0, PAGE_SIZE));
		}

		LOG.info("Hash computation completed. Processed {} attachments", totalProcessed);
	}
}

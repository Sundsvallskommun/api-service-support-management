package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.model.IdProjection;
import se.sundsvall.supportmanagement.service.util.ServiceUtil;

@Component
public class AttachmentHashWorker {

	private static final Logger LOG = LoggerFactory.getLogger(AttachmentHashWorker.class);

	private final AttachmentRepository attachmentRepository;
	private final TransactionTemplate transactionTemplate;
	private final int batchSize;
	private final Duration maxExecutionTime;

	public AttachmentHashWorker(final AttachmentRepository attachmentRepository,
		final PlatformTransactionManager transactionManager,
		@Value("${scheduler.attachment-hash.batch-size:250}") final int batchSize,
		@Value("${scheduler.attachment-hash.maximum-execution-time:PT5M}") final Duration maxExecutionTime) {
		this.attachmentRepository = attachmentRepository;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		this.transactionTemplate.setTimeout(30);
		this.batchSize = batchSize;
		this.maxExecutionTime = maxExecutionTime;
	}

	public void computeHashForAttachmentsWithoutHash() {
		final var startTime = Instant.now();
		final var attachmentIds = attachmentRepository.findByHashIsNull(Pageable.ofSize(batchSize)).stream()
			.map(IdProjection::getId)
			.toList();

		if (attachmentIds.isEmpty()) {
			LOG.info("No attachments without hash found");
			return;
		}

		LOG.info("Found {} attachments without hash, starting hash computation", attachmentIds.size());

		var totalProcessed = 0;
		var totalFailed = 0;

		for (final var attachmentId : attachmentIds) {
			if (Thread.currentThread().isInterrupted() || Duration.between(startTime, Instant.now()).compareTo(maxExecutionTime) >= 0) {
				LOG.info("Time limit reached, stopping. Processed {} attachments successfully, {} failed", totalProcessed, totalFailed);
				return;
			}
			if (processAttachment(attachmentId)) {
				totalProcessed++;
			} else {
				totalFailed++;
			}
		}

		LOG.info("Hash computation completed. Processed {} attachments successfully, {} failed", totalProcessed, totalFailed);
	}

	private boolean processAttachment(final String attachmentId) {
		try {
			return Boolean.TRUE.equals(transactionTemplate.execute(_ -> {
				final var attachment = attachmentRepository.findById(attachmentId).orElse(null);
				if (attachment == null) {
					LOG.warn("Attachment with id: {} no longer exists, skipping", attachmentId);
					return false;
				}
				try {
					final var blob = attachment.getAttachmentData().getFile();
					final var hash = ServiceUtil.computeSha256Hex(blob.getBinaryStream());
					attachment.setHash(hash);
					attachmentRepository.saveAndFlush(attachment);
					return true;
				} catch (final Exception e) {
					LOG.warn("Failed to compute hash for attachment with id: {}", attachmentId, e);
					return false;
				}
			}));
		} catch (final Exception e) {
			LOG.warn("Failed to process attachment with id: {}", attachmentId, e);
			return false;
		}
	}
}

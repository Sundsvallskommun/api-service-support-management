package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.service.util.ServiceUtil;

@Component
public class AttachmentHashBatchProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(AttachmentHashBatchProcessor.class);

	private final AttachmentRepository attachmentRepository;
	private final EntityManager entityManager;

	public AttachmentHashBatchProcessor(final AttachmentRepository attachmentRepository, final EntityManager entityManager) {
		this.attachmentRepository = attachmentRepository;
		this.entityManager = entityManager;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int processBatch(final List<String> attachmentIds) {
		var processed = 0;

		// Load, hash and detach one attachment at a time. Holding the whole batch would keep every
		// materialized blob reachable (via the entity graph) for the entire loop, so peak heap would
		// grow to the sum of all blobs in the batch. Processing one at a time bounds it to a single blob.
		for (final var attachmentId : attachmentIds) {
			final var attachment = attachmentRepository.findById(attachmentId).orElse(null);
			if (attachment == null) {
				LOG.warn("Attachment with id: {} no longer exists, skipping", attachmentId);
				continue;
			}

			try {
				final var blob = attachment.getAttachmentData().getFile();
				final var hash = ServiceUtil.computeSha256Hex(blob.getBinaryStream());
				attachment.setHash(hash);
				attachmentRepository.saveAndFlush(attachment);
				processed++;
			} catch (final Exception e) {
				LOG.warn("Failed to compute hash for attachment with id: {}", attachmentId, e);
			} finally {
				// Detach so the entity and its blob leave the persistence context; the next iteration then
				// overwrites the local reference, making the previous blob unreachable and GC-eligible.
				entityManager.detach(attachment);
			}
		}

		return processed;
	}
}

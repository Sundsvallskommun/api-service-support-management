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
		final var attachments = attachmentRepository.findAllById(attachmentIds);
		var processed = 0;

		for (final var attachment : attachments) {
			try {
				final var blob = attachment.getAttachmentData().getFile();
				final var hash = ServiceUtil.computeSha256Hex(blob.getBinaryStream());
				attachment.setHash(hash);
				attachmentRepository.save(attachment);
				processed++;
			} catch (final Exception e) {
				LOG.warn("Failed to compute hash for attachment with id: {}", attachment.getId(), e);
			} finally {
				entityManager.detach(attachment);
			}
		}

		return processed;
	}
}

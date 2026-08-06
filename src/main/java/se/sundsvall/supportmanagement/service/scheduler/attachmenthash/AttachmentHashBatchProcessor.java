package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

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

	public AttachmentHashBatchProcessor(final AttachmentRepository attachmentRepository) {
		this.attachmentRepository = attachmentRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean processAttachment(final String attachmentId) {
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
	}
}

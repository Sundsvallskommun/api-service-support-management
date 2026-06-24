package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.service.util.ServiceUtil;

@Component
public class AttachmentHashWorker {

	private static final Logger LOG = LoggerFactory.getLogger(AttachmentHashWorker.class);

	private final AttachmentRepository attachmentRepository;

	public AttachmentHashWorker(final AttachmentRepository attachmentRepository) {
		this.attachmentRepository = attachmentRepository;
	}

	@Transactional
	public void computeHashForAttachmentsWithoutHash() {
		final var attachments = attachmentRepository.findByHashIsNull();

		if (attachments.isEmpty()) {
			LOG.info("No attachments without hash found");
			return;
		}

		LOG.info("Found {} attachments without hash, starting hash computation", attachments.size());

		var processedCount = 0;
		for (final AttachmentEntity attachment : attachments) {
			try {
				final var blob = attachment.getAttachmentData().getFile();
				final var hash = ServiceUtil.computeSha256Hex(blob.getBinaryStream());
				attachment.setHash(hash);
				attachmentRepository.save(attachment);
				processedCount++;
			} catch (final Exception e) {
				LOG.warn("Failed to compute hash for attachment with id: {}", attachment.getId(), e);
			}
		}

		LOG.info("Hash computation completed. Processed {} of {} attachments", processedCount, attachments.size());
	}
}

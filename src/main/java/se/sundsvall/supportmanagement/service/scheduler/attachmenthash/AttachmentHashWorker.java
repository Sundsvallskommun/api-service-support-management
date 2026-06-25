package se.sundsvall.supportmanagement.service.scheduler.attachmenthash;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.service.util.ServiceUtil;

@Component
public class AttachmentHashWorker {

	private static final int PAGE_SIZE = 100;
	private static final Logger LOG = LoggerFactory.getLogger(AttachmentHashWorker.class);

	private final AttachmentRepository attachmentRepository;

	public AttachmentHashWorker(final AttachmentRepository attachmentRepository) {
		this.attachmentRepository = attachmentRepository;
	}

	@Transactional
	public void computeHashForAttachmentsWithoutHash() {
		var totalProcessed = 0;
		var page = attachmentRepository.findByHashIsNull(PageRequest.of(0, PAGE_SIZE));

		if (page.isEmpty()) {
			LOG.info("No attachments without hash found");
			return;
		}

		LOG.info("Found {} attachments without hash, starting hash computation", page.getTotalElements());

		while (!page.isEmpty()) {
			final var updatedAttachments = new ArrayList<AttachmentEntity>();

			for (final AttachmentEntity attachment : page.getContent()) {
				try {
					final var blob = attachment.getAttachmentData().getFile();
					final var hash = ServiceUtil.computeSha256Hex(blob.getBinaryStream());
					attachment.setHash(hash);
					updatedAttachments.add(attachment);
				} catch (final Exception e) {
					LOG.warn("Failed to compute hash for attachment with id: {}", attachment.getId(), e);
				}
			}

			if (!updatedAttachments.isEmpty()) {
				attachmentRepository.saveAll(updatedAttachments);
				totalProcessed += updatedAttachments.size();
			}

			if (page.hasNext()) {
				page = attachmentRepository.findByHashIsNull(PageRequest.of(0, PAGE_SIZE));
			} else {
				break;
			}
		}

		LOG.info("Hash computation completed. Processed {} attachments", totalProcessed);
	}
}

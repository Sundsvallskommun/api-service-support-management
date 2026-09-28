package se.sundsvall.supportmanagement.integration.db.util;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.AttachmentSequenceRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentSequenceEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.SequenceNumberProjection;

@Component
public class AttachmentSequenceNumberGenerator {

	private final AttachmentSequenceRepository attachmentSequenceRepository;
	private final AttachmentRepository attachmentRepository;
	private final ErrandsRepository errandsRepository;

	public AttachmentSequenceNumberGenerator(final AttachmentSequenceRepository attachmentSequenceRepository, final AttachmentRepository attachmentRepository, final ErrandsRepository errandsRepository) {
		this.attachmentSequenceRepository = attachmentSequenceRepository;
		this.attachmentRepository = attachmentRepository;
		this.errandsRepository = errandsRepository;
	}

	/**
	 * Starts the sequence of a new errand, so that its first attachment is given number 1.
	 * <p>
	 * Must be called when the errand is created, within the transaction that saves it. Without a sequence, two errands
	 * given their first attachment at the same time can deadlock each other in {@link #nextSequenceNumber}.
	 *
	 * @param errandEntity the errand just saved.
	 */
	@Transactional
	public void startSequence(final ErrandEntity errandEntity) {
		attachmentSequenceRepository.save(AttachmentSequenceEntity.create()
			.withErrandEntity(errandEntity)
			.withLastSequenceNumber(0));
	}

	/**
	 * Gives the next sequence number for an attachment of the errand: one more than the last number given, starting at 1.
	 * A number is never given twice within the errand, also after the attachment carrying it has been removed.
	 * <p>
	 * Locks the errand row until the surrounding transaction ends, so concurrent additions to the same errand are given
	 * numbers one at a time. Must be called within the transaction that saves the attachment.
	 * <p>
	 * An errand without a sequence, which {@link #startSequence} is there to prevent, is given one that continues from the
	 * highest number among its attachments.
	 *
	 * @param  errandEntity the errand the attachment is added to.
	 * @return              the sequence number to give the attachment.
	 */
	@Transactional
	public int nextSequenceNumber(final ErrandEntity errandEntity) {
		final var errandId = errandEntity.getId();
		errandsRepository.existsWithLockingByIdAndNamespaceAndMunicipalityId(errandId, errandEntity.getNamespace(), errandEntity.getMunicipalityId());

		final var sequence = attachmentSequenceRepository.findByErrandId(errandId)
			.orElseGet(() -> AttachmentSequenceEntity.create()
				.withErrandEntity(errandsRepository.getReferenceById(errandId))
				.withLastSequenceNumber(highestSequenceNumber(errandId)));

		sequence.setLastSequenceNumber(sequence.getLastSequenceNumber() + 1);
		attachmentSequenceRepository.save(sequence);

		return sequence.getLastSequenceNumber();
	}

	private int highestSequenceNumber(final String errandId) {
		return attachmentRepository.findTopByErrandEntityIdOrderBySequenceNumberDesc(errandId)
			.map(SequenceNumberProjection::getSequenceNumber)
			.orElse(0);
	}
}

package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.AttachmentDataRepository;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.HandoverIdempotencyRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriberNotificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataIdProjection;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Removes everything that hangs off an errand and is not swept away by deleting the errand row itself.
 * <p>
 * Shared by the single errand delete and by the retention purge so that the two cannot drift apart: whatever one of
 * them cleans up the other cleans up too, and a table added here is covered by both from the moment it is added.
 * <p>
 * Notes live in a neighbouring service and are guarded here. An errand must not survive because that service is down,
 * so a failure there is logged and the errand is removed regardless: an orphaned note is a smaller problem than an
 * errand that cannot be deleted at all.
 * <p>
 * Conversations are not guarded here, even though they too reach a neighbouring service. Every call that leaves the
 * service is already caught inside {@link ConversationService#deleteByErrandId(ErrandEntity)}, one conversation and
 * one relation at a time. What can still reach this class is therefore a write to the local database, and by then the
 * transaction the removal runs in is already marked for rollback. Catching it would not let the errand go. It would
 * only hide why it stayed, and leave a caller with a delete that answered success while removing nothing.
 */
@Component
public class ErrandDataDeleter {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandDataDeleter.class);

	private static final int NOTE_PAGE_SIZE = 100;
	private static final int MAX_NOTE_ROUNDS = 100;

	private final ConversationService conversationService;
	private final CommunicationService communicationService;
	private final AttachmentRepository attachmentRepository;
	private final AttachmentDataRepository attachmentDataRepository;
	private final NotesClient notesClient;
	private final SubscriberNotificationRepository subscriberNotificationRepository;
	private final HandoverIdempotencyRepository handoverIdempotencyRepository;
	private final EntityManager entityManager;
	private final ChunkedDeleter chunkedDeleter;

	public ErrandDataDeleter(
		final ConversationService conversationService,
		final CommunicationService communicationService,
		final AttachmentRepository attachmentRepository,
		final AttachmentDataRepository attachmentDataRepository,
		final NotesClient notesClient,
		final SubscriberNotificationRepository subscriberNotificationRepository,
		final HandoverIdempotencyRepository handoverIdempotencyRepository,
		final EntityManager entityManager,
		final ChunkedDeleter chunkedDeleter) {

		this.conversationService = conversationService;
		this.communicationService = communicationService;
		this.attachmentRepository = attachmentRepository;
		this.attachmentDataRepository = attachmentDataRepository;
		this.notesClient = notesClient;
		this.subscriberNotificationRepository = subscriberNotificationRepository;
		this.handoverIdempotencyRepository = handoverIdempotencyRepository;
		this.entityManager = entityManager;
		this.chunkedDeleter = chunkedDeleter;
	}

	/**
	 * Removes everything belonging to the errand except the errand row, which the caller deletes once this returns.
	 * <p>
	 * Attachment ids are passed in rather than read here, since the two callers reach them differently: a delete reads
	 * them through the access check that guards attachments, while a purge takes them straight off the entity because it
	 * runs with no caller to authorize.
	 * <p>
	 * Relations are left alone. The ones a conversation owns are removed by the conversation itself, and the rest are the
	 * relation service's to keep or drop.
	 * <p>
	 * <b>The errand is detached by the time this returns</b>, deliberately and in every case. Removing the
	 * communications empties the persistence context to keep an entire correspondence from piling up in the heap, and
	 * the attachments are removed with the errand out of the context so that nothing is cascaded back into place.
	 * Everything read off the errand here is therefore read up front, and a caller needing more of it afterwards has to
	 * read that before calling.
	 *
	 * @param entity        the errand being removed.
	 * @param attachmentIds ids of the attachments to remove along with it.
	 */
	public void deleteRelatedData(final ErrandEntity entity, final List<String> attachmentIds) {
		final var errandId = entity.getId();
		final var municipalityId = entity.getMunicipalityId();
		final var namespace = entity.getNamespace();
		final var errandNumber = entity.getErrandNumber();

		conversationService.deleteByErrandId(entity);

		communicationService.deleteAllCommunicationsByErrandNumber(errandNumber, namespace, municipalityId);

		// After the communications, and that order is not a matter of taste. A communication can arrive carrying an
		// attachment, and the copy kept on the errand points at the very same blob. Removing the errand's attachment
		// takes that blob with it through the cascade on its data, so doing it first would pull the blob out from under
		// a communication attachment still pointing at it.
		//
		// The errand is taken out of the persistence context first. An attachment left in the collection of a managed
		// errand is resurrected by the cascade on the next flush and written back with its data reference nulled, which
		// the column refuses. Detaching says that once and holds however the removal above happened to leave the
		// context, which reaching into the collection to take the attachments out of it would not.
		entityManager.detach(entity);

		deleteAttachments(ofNullable(attachmentIds).orElse(emptyList()));

		deleteNotes(municipalityId, errandId);

		// Notifications sent to subscribers point at an errand that is about to stop existing, and each takes its events
		// with it through the cascade on the notification.
		subscriberNotificationRepository.deleteAllByErrandId(errandId);

		// A handover is recorded against both ends, and the errand being removed may be either of them.
		handoverIdempotencyRepository.deleteAllBySourceErrandIdOrNewErrandId(errandId, errandId);
	}

	/**
	 * Removes the attachments of an errand together with the files they hold.
	 * <p>
	 * The rows are named rather than loaded, and that is what this method is for. An attachment cascades its removal
	 * onto its data, and a cascade reaches the data by loading it - which means the whole file in the heap, for every
	 * attachment of the errand at once, held until the transaction commits. An errand may carry fifty megabytes per
	 * attachment and any number of them, so what that costs is set by what somebody once uploaded rather than by
	 * anything this service controls.
	 * <p>
	 * The ids of the data rows are therefore read without the files, and both tables are then emptied of those rows by
	 * removals that load nothing. The attachments go first, since they are the ones holding the foreign key. Doing it
	 * this way means the cascade is bypassed rather than relied on, which is why the data rows are removed here
	 * explicitly instead of being left to follow.
	 *
	 * @param attachmentIds ids of the attachments to remove.
	 */
	private void deleteAttachments(final List<String> attachmentIds) {
		if (attachmentIds.isEmpty()) {
			return;
		}

		// Read before anything is removed: once the attachment rows are gone there is nothing left to say which data
		// rows they pointed at.
		final var attachmentDataIds = attachmentRepository.findByIdIn(attachmentIds).stream()
			.map(AttachmentDataIdProjection::getAttachmentDataId)
			.toList();

		chunkedDeleter.deleteInChunks(attachmentIds, attachmentRepository::deleteAllByIdInBatch);
		chunkedDeleter.deleteInChunks(attachmentDataIds, attachmentDataRepository::deleteAllByIdInBatch);
	}

	/**
	 * Removes the notes of an errand, however many there are.
	 * <p>
	 * The first page is asked for over and over rather than the pages being walked, since every note that is read is also
	 * removed and the notes behind it move up to take its place. A page that did not fill up is the last one: what was
	 * read has just been removed, so anything still there would have been on that page.
	 * <p>
	 * The rounds are capped all the same. A note reported as removed that comes back on the next read would otherwise
	 * keep the caller here for good, and a purge is carried out one errand at a time by a single thread.
	 */
	private void deleteNotes(final String municipalityId, final String errandId) {
		try {
			for (var round = 0; round < MAX_NOTE_ROUNDS; round++) {
				final var notes = ofNullable(notesClient.findNotes(municipalityId, null, null, errandId, null, null, 1, NOTE_PAGE_SIZE).getNotes())
					.orElse(emptyList());

				notes.forEach(note -> notesClient.deleteNoteById(municipalityId, note.getId()));

				if (notes.size() < NOTE_PAGE_SIZE) {
					return;
				}
			}

			LOG.warn("Gave up removing notes for errand {} after {} rounds of {}, since they kept being returned as still there",
				sanitizeForLogging(errandId), MAX_NOTE_ROUNDS, NOTE_PAGE_SIZE);
		} catch (final Exception e) {
			LOG.warn("Failed to delete notes for errand {}: {}", sanitizeForLogging(errandId), e.getMessage());
		}
	}
}

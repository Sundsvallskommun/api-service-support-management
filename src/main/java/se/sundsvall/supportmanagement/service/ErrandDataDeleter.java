package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.HandoverIdempotencyRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriberNotificationRepository;
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
 * The parts that live in neighbouring services - conversations and notes - are each guarded on their own. An errand
 * must not survive because another service is down, so a failure there is logged and the errand is removed regardless.
 * An orphaned note is a smaller problem than an errand that cannot be deleted at all.
 */
@Component
public class ErrandDataDeleter {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandDataDeleter.class);

	private static final int NOTE_PAGE_SIZE = 100;
	private static final int MAX_NOTE_ROUNDS = 100;

	private final ConversationService conversationService;
	private final CommunicationService communicationService;
	private final AttachmentRepository attachmentRepository;
	private final NotesClient notesClient;
	private final SubscriberNotificationRepository subscriberNotificationRepository;
	private final HandoverIdempotencyRepository handoverIdempotencyRepository;

	public ErrandDataDeleter(
		final ConversationService conversationService,
		final CommunicationService communicationService,
		final AttachmentRepository attachmentRepository,
		final NotesClient notesClient,
		final SubscriberNotificationRepository subscriberNotificationRepository,
		final HandoverIdempotencyRepository handoverIdempotencyRepository) {

		this.conversationService = conversationService;
		this.communicationService = communicationService;
		this.attachmentRepository = attachmentRepository;
		this.notesClient = notesClient;
		this.subscriberNotificationRepository = subscriberNotificationRepository;
		this.handoverIdempotencyRepository = handoverIdempotencyRepository;
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
	 *
	 * @param entity        the errand being removed.
	 * @param attachmentIds ids of the attachments to remove along with it.
	 */
	public void deleteRelatedData(final ErrandEntity entity, final List<String> attachmentIds) {
		final var errandId = entity.getId();
		final var municipalityId = entity.getMunicipalityId();

		deleteConversations(entity);

		communicationService.deleteAllCommunicationsByErrandNumber(entity.getErrandNumber(), entity.getNamespace(), municipalityId);

		// Taken out of the errand before the rows are removed. The errand is still managed here, and the removals further
		// down query the database, which flushes. An attachment left in the collection of a managed errand is resurrected
		// by the cascade on that flush and written back with its data reference nulled, which the column refuses.
		final var idsToRemove = ofNullable(attachmentIds).orElse(emptyList());
		ofNullable(entity.getAttachments()).ifPresent(attachments -> attachments.removeIf(attachment -> idsToRemove.contains(attachment.getId())));
		idsToRemove.forEach(attachmentRepository::deleteById);

		deleteNotes(municipalityId, errandId);

		// Notifications sent to subscribers point at an errand that is about to stop existing, and each takes its events
		// with it through the cascade on the notification.
		subscriberNotificationRepository.deleteAllByErrandId(errandId);

		// A handover is recorded against both ends, and the errand being removed may be either of them.
		handoverIdempotencyRepository.deleteAllBySourceErrandIdOrNewErrandId(errandId, errandId);
	}

	private void deleteConversations(final ErrandEntity entity) {
		try {
			conversationService.deleteByErrandId(entity);
		} catch (final Exception e) {
			LOG.warn("Failed to delete conversations for errand {}: {}", sanitizeForLogging(entity.getId()), e.getMessage());
		}
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

package se.sundsvall.supportmanagement.service;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportmanagement.service.mapper.EventlogMapper.toEvent;
import static se.sundsvall.supportmanagement.service.mapper.EventlogMapper.toMetadataMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import se.sundsvall.supportmanagement.api.filter.ExecutingUserSupplier;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.event.Event;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.eventlog.EventlogClient;
import se.sundsvall.supportmanagement.service.mapper.EventlogMapper;

import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.notes.Note;

@Service
public class EventService {

	private final ExecutingUserSupplier executingUserSupplier;

	private final EventlogClient eventLogClient;

	public EventService(final ExecutingUserSupplier executingUserSupplier,
		final EventlogClient eventLogClient) {
		this.executingUserSupplier = executingUserSupplier;
		this.eventLogClient = eventLogClient;
	}

	public void createErrandEvent(EventType eventType, String message, ErrandEntity errandEntity, Revision currentRevision, Revision previousRevision) {
		final var metadata = toMetadataMap(errandEntity, currentRevision, previousRevision);
		eventLogClient.createEvent(
			errandEntity.getId(),
			toEvent(eventType, message, extractId(currentRevision), Errand.class, metadata, executingUserSupplier.getAdUser()));
	}

	public void createErrandNoteEvent(EventType eventType, String message, String logKey, String caseId, String noteId, Revision currentRevision, Revision previousRevision) {
		final var metadata = toMetadataMap(caseId, noteId, currentRevision, previousRevision);
		eventLogClient.createEvent(
			logKey,
			toEvent(eventType, message, extractId(currentRevision), Note.class, metadata, executingUserSupplier.getAdUser()));
	}

	public Page<Event> readEvents(String id, Pageable pageable) {
		final var response = eventLogClient.getEvents(id, pageable);

		return new PageImpl<>(response.getContent().stream()
			.map(EventlogMapper::toEvent)
			.filter(event -> nonNull(event.getType()))
			.toList(), pageable, response.getTotalElements());
	}

	private String extractId(Revision currentRevision) {
		return ofNullable(currentRevision).map(Revision::getId).orElse(null);
	}
}

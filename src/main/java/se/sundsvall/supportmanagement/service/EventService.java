package se.sundsvall.supportmanagement.service;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportmanagement.service.mapper.EventlogMapper.toEvent;
import static se.sundsvall.supportmanagement.service.mapper.EventlogMapper.toMetadataMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import generated.se.sundsvall.eventlog.EventType;
import se.sundsvall.supportmanagement.api.model.event.Event;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.eventlog.EventlogClient;
import se.sundsvall.supportmanagement.service.mapper.EventlogMapper;

@Service
public class EventService {

	@Autowired
	private EventlogClient eventLogClient;

	public void createEvent(EventType eventType, String message, ErrandEntity errandEntity, Revision currentRevision, Revision previousRevision, String executedByUserId) {

		final var metadata = toMetadataMap(errandEntity, currentRevision, previousRevision);
		eventLogClient.createEvent(
			errandEntity.getId(),
			toEvent(eventType, message, ofNullable(currentRevision).map(Revision::getId).orElse(null), metadata, executedByUserId));
	}

	public Page<Event> readEvents(String id, Pageable pageable) {
		final var response = eventLogClient.getEvents(id, pageable);

		return new PageImpl<>(response.getContent().stream()
			.map(EventlogMapper::toEvent)
			.filter(event -> nonNull(event.getType()))
			.toList(), pageable, response.getTotalElements());
	}
}

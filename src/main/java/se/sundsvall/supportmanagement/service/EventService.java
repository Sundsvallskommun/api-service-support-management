package se.sundsvall.supportmanagement.service;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportmanagement.Constants.EXTERNAL_TAG_KEY_CASE_ID;
import static se.sundsvall.supportmanagement.service.mapper.EventlogMapper.toEvent;
import static se.sundsvall.supportmanagement.service.mapper.EventlogMapper.toMetadataMap;
import static se.sundsvall.supportmanagement.service.mapper.NotificationMapper.toNotification;

import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import generated.se.sundsvall.eventlog.EventType;
import generated.se.sundsvall.notes.Note;
import se.sundsvall.supportmanagement.api.filter.ExecutingUserSupplier;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.event.Event;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.eventlog.EventlogClient;
import se.sundsvall.supportmanagement.service.mapper.EventlogMapper;

@Service
public class EventService {

	private final ExecutingUserSupplier executingUserSupplier;

	private final EventlogClient eventLogClient;

	private final NotificationService notificationService;

	private final EmployeeService employeeService;

	public EventService(final ExecutingUserSupplier executingUserSupplier,
		final EventlogClient eventLogClient, final NotificationService notificationService, final EmployeeService employeeService) {
		this.executingUserSupplier = executingUserSupplier;
		this.eventLogClient = eventLogClient;
		this.notificationService = notificationService;

		this.employeeService = employeeService;
	}

	public void createErrandEvent(final EventType eventType, final String message, final ErrandEntity errandEntity, final Revision currentRevision, final Revision previousRevision, boolean sendNotification) {
		final var metadata = toMetadataMap(errandEntity, currentRevision, previousRevision);
		final var event = toEvent(eventType, message, extractId(currentRevision), Errand.class, metadata, executingUserSupplier.getAdUser());
		eventLogClient.createEvent(errandEntity.getMunicipalityId(), errandEntity.getId(), event);

		if (sendNotification) {
			createNotification(errandEntity, event);
		}
	}

	public void createErrandEvent(final EventType eventType, final String message, final ErrandEntity errandEntity, final Revision currentRevision, final Revision previousRevision) {
		createErrandEvent(eventType, message, errandEntity, currentRevision, previousRevision, true);
	}

	public void createErrandNoteEvent(final EventType eventType, final String message, final String logKey, final ErrandEntity errandEntity, final String noteId, final Revision currentRevision, final Revision previousRevision) {
		final var caseId = extractCaseId(errandEntity);
		final var metadata = toMetadataMap(caseId, noteId, currentRevision, previousRevision);
		final var event = toEvent(eventType, message, extractId(currentRevision), Note.class, metadata, executingUserSupplier.getAdUser());
		eventLogClient.createEvent(errandEntity.getMunicipalityId(), logKey, event);
		createNotification(errandEntity, event);

	}

	public Page<Event> readEvents(final String municipalityId, final String id, final Pageable pageable) {
		final var response = eventLogClient.getEvents(municipalityId, id, pageable);

		return new PageImpl<>(response.getContent().stream()
			.map(EventlogMapper::toEvent)
			.filter(event -> nonNull(event.getType()))
			.toList(), pageable, response.getTotalElements());
	}

	private String extractId(final Revision currentRevision) {
		return ofNullable(currentRevision).map(Revision::getId).orElse(null);
	}

	private void createNotification(final ErrandEntity errandEntity, final generated.se.sundsvall.eventlog.Event event) {

		Optional.ofNullable(errandEntity.getAssignedUserId()).ifPresent(loginName -> {
			final var owner = employeeService.getEmployeeByLoginName(loginName);
			final var creator = employeeService.getEmployeeByLoginName(executingUserSupplier.getAdUser());

			final var notification = toNotification(event, errandEntity, owner, creator, executingUserSupplier.getAdUser());
			notificationService.createNotification(errandEntity.getMunicipalityId(), errandEntity.getNamespace(), errandEntity.getId(), notification);
		});

	}

	private String extractCaseId(final ErrandEntity errand) {
		return ofNullable(errand)
			.map(ErrandEntity::getExternalTags)
			.orElse(emptyList())
			.stream()
			.filter(et -> Objects.equals(EXTERNAL_TAG_KEY_CASE_ID, et.getKey()))
			.map(DbExternalTag::getValue)
			.findAny()
			.orElse(null);

	}

}

package se.sundsvall.supportmanagement.service.mapper;

import generated.se.sundsvall.pwalkt.ErrandEvent;
import generated.se.sundsvall.pwalkt.ErrandEvent.EventTypeEnum;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;

public final class ProcessEventMapper {

	private ProcessEventMapper() {}

	/**
	 * The event pw-alkt is sent for a row of the outbox.
	 * <p>
	 * Every value is copied from the row as it was written, the process key, the start permission and the signal name
	 * included. The id of the row is the id of the event, through which pw-alkt recognises an event it has been given
	 * before.
	 *
	 * @param  row the row to deliver.
	 * @return     the event for the row.
	 */
	public static ErrandEvent toErrandEvent(final ProcessEventOutboxEntity row) {
		return new ErrandEvent()
			.eventId(row.getId())
			.eventType(EventTypeEnum.fromValue(row.getEventType()))
			.eventSubType(row.getEventSubType())
			.errandId(row.getErrandId())
			.processKey(row.getProcessKey())
			.startAllowed(row.isStartAllowed())
			.signalName(row.getSignalName())
			.occurredAt(row.getCreated());
	}
}

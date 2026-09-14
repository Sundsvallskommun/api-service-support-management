package se.sundsvall.supportmanagement.service.mapper;

import generated.se.sundsvall.pwalkt.ErrandEvent;
import generated.se.sundsvall.pwalkt.ErrandEvent.EventTypeEnum;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;

public final class ProcessEventMapper {

	private ProcessEventMapper() {}

	/**
	 * The event pw-alkt is sent for a row of the outbox.
	 * <p>
	 * Copied from the row, with nothing worked out here. The process key, the start permission and the signal name were
	 * settled when the row was written, and a delivery that decided any of them again could decide differently on a
	 * retry. The id of the row is the id of the event, which is what lets pw-alkt recognise an event it has been given
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

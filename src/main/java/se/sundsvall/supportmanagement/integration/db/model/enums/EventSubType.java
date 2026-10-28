package se.sundsvall.supportmanagement.integration.db.model.enums;

import java.util.Arrays;
import java.util.Objects;

public enum EventSubType {

	ASSIGNMENT,
	ATTACHMENT,
	DECISION,
	ERRAND,
	ERRAND_ACTIVATION,
	HANDOVER_IN,
	HANDOVER_OUT,
	LABEL,
	MESSAGE_CONVERSATION_EXTERNAL,
	MESSAGE_CONVERSATION_INTERNAL,
	MESSAGE_EMAIL_EXTERNAL,
	MESSAGE_SMS_EXTERNAL,
	MESSAGE_WEB_MESSAGE_EXTERNAL,
	NOTE,
	PHASE(ERRAND),
	PROCESS,
	SIGNAL,
	STATUS(ERRAND),
	SYSTEM,
	SUSPENSION;

	/**
	 * The subtype this one refines, or null. A refined subtype is still one of the subtype it refines, so a filter on that
	 * subtype selects it too.
	 */
	private final EventSubType refines;

	EventSubType() {
		this(null);
	}

	EventSubType(final EventSubType refines) {
		this.refines = refines;
	}

	public String getValue() {
		return this.name();
	}

	/**
	 * Whether the event is a request aimed at the process rather than something that happened to the errand: the manual
	 * start or the manual step. Commands pass the process triggers and the emergency brake.
	 *
	 * @return true for the two command sub types
	 */
	public boolean isCommand() {
		return this == PROCESS || this == SIGNAL;
	}

	/**
	 * Signals whether a filter on the given subtype selects an event of the given subtype. A filter on a subtype selects
	 * the events of that subtype and of every subtype refining it, as ERRAND selects changes of status and of
	 * phase, while a filter on a refined subtype selects only that one.
	 *
	 * @param  filterSubtype the subtype the filter declares
	 * @param  eventSubtype  the subtype of the event
	 * @return               true if the filter selects the event
	 */
	public static boolean selects(final String filterSubtype, final String eventSubtype) {
		return Arrays.stream(values())
			.filter(subtype -> subtype.name().equals(eventSubtype))
			.findFirst()
			.map(subtype -> subtype.isSelectedBy(filterSubtype))
			.orElseGet(() -> Objects.equals(filterSubtype, eventSubtype));
	}

	private boolean isSelectedBy(final String filterSubtype) {
		return name().equals(filterSubtype) || (refines != null && refines.isSelectedBy(filterSubtype));
	}
}

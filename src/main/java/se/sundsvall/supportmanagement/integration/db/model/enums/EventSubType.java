package se.sundsvall.supportmanagement.integration.db.model.enums;

import java.util.Arrays;
import java.util.Objects;

public enum EventSubType {

	ASSIGNMENT,
	ATTACHMENT,
	DECISION,
	ERRAND,
	HANDOVER_IN,
	HANDOVER_OUT,
	MESSAGE,
	MESSAGE_EXTERNAL(MESSAGE),
	MESSAGE_INTERNAL(MESSAGE),
	NOTE,
	PHASE(ERRAND),
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
	 * Signals whether a filter on the given subtype selects an event of the given subtype. A filter on a subtype selects
	 * the events of that subtype and of every subtype refining it, as MESSAGE selects both internal and external
	 * messages, while a filter on a refined subtype selects only that one.
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

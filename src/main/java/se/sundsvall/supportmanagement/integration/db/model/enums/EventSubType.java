package se.sundsvall.supportmanagement.integration.db.model.enums;

public enum EventSubType {

	ATTACHMENT,
	DECISION,
	ERRAND,
	HANDOVER_IN,
	HANDOVER_OUT,
	MESSAGE,
	NOTE,
	PROCESS,
	SIGNAL,
	SYSTEM,
	SUSPENSION;

	public String getValue() {
		return this.name();
	}

	/**
	 * Whether the event is a request aimed at the process rather than something that happened to the errand.
	 * <p>
	 * The manual start and the manual step are both a person pressing a button, which is why they pass the process
	 * triggers and the emergency brake: neither an errand change to be filtered nor a loop to be broken.
	 *
	 * @return true for the two command sub types
	 */
	public boolean isCommand() {
		return this == PROCESS || this == SIGNAL;
	}

}

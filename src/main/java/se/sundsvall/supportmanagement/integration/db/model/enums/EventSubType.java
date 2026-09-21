package se.sundsvall.supportmanagement.integration.db.model.enums;

public enum EventSubType {

	ATTACHMENT,
	DECISION,
	ERRAND,
	HANDOVER_IN,
	HANDOVER_OUT,
	MESSAGE,
	NOTE,
	RESTRICTED,
	SYSTEM,
	SUSPENSION;

	public String getValue() {
		return this.name();
	}

}

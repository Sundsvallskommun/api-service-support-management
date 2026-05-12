package se.sundsvall.supportmanagement.integration.db.model.enums;

public enum EventSubType {

	ATTACHMENT,
	DECISION,
	ERRAND,
	MESSAGE,
	NOTE,
	SYSTEM,
	SUSPENSION;

	public String getValue() {
		return this.name();
	}

}

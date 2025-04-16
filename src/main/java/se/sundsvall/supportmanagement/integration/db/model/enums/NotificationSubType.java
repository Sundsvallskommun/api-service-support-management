package se.sundsvall.supportmanagement.integration.db.model.enums;

public enum NotificationSubType {

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

package se.sundsvall.supportmanagement.integration.db.model.enums;

public enum NotificationSubType {

	ATTACHMENT("ATTACHMENT"),
	DECISION("DECISION"),
	ERRAND("ERRAND"),
	MESSAGE("MESSAGE"),
	NOTE("NOTE"),
	SYSTEM("SYSTEM"),
	SUSPENSION("SUSPENSION");

	private final String value;

	NotificationSubType(final String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}

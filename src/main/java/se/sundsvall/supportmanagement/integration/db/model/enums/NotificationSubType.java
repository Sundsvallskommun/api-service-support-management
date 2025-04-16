package se.sundsvall.supportmanagement.integration.db.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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

	@JsonCreator
	public static NotificationSubType fromValue(final String value) {
		for (final NotificationSubType b : NotificationSubType.values()) {
			if (b.value.equals(value)) {
				return b;
			}
		}
		throw new IllegalArgumentException("Unexpected value '" + value + "'");
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

}

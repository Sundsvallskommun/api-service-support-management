package se.sundsvall.supportmanagement.integration.db.model.enums;

import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;

public enum ValueType {
	BOOLEAN,
	STRING,
	INTEGER;

	public static Object getAsTypedClass(NamespaceConfigValueEmbeddable configValue) {
		return switch (configValue.getType()) {
			case BOOLEAN -> Boolean.valueOf(configValue.getValue());
			case INTEGER -> Integer.valueOf(configValue.getValue());
			case STRING -> String.valueOf(configValue.getValue());
		};
	}

}

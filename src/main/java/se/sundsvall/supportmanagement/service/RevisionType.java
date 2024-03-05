package se.sundsvall.supportmanagement.service;

public enum RevisionType {
	CURRENT("current"),
	PREVIOUS("previous");

	private final String value;

	RevisionType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}

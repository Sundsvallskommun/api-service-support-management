package se.sundsvall.supportmanagement.service;

public enum RevisionType {
	CURRENT("current"),
	PREVIOUS("previous");

	private String value;

	private RevisionType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}

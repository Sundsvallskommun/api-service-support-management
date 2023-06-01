package se.sundsvall.supportmanagement;

public class Constants {
	private Constants() {}

	public static final String NAMESPACE_REGEXP = "[\\w|\\.]+";
	public static final String NAMESPACE_VALIDATON_MESSAGE = "can only contain A-Z, a-z, 0-9, -, _ and .";
	public static final String AD_USER_HEADER_KEY = "sentbyuser";

	public static final String EXTERNAL_TAG_KEY_CASE_ID = "CaseId";
	public static final String EXTERNAL_TAG_KEY_EXECUTED_BY = "ExecutedBy";
	public static final String EXTERNAL_TAG_KEY_PREVIOUS_REVISION = "PreviousRevision";
	public static final String EXTERNAL_TAG_KEY_PREVIOUS_VERSION = "PreviousVersion";
	public static final String EXTERNAL_TAG_KEY_CURRENT_REVISION = "CurrentRevision";
	public static final String EXTERNAL_TAG_KEY_CURRENT_VERSION = "CurrentVersion";
}

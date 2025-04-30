package se.sundsvall.supportmanagement;

public final class Constants {

	private Constants() {}

	public static final String NAMESPACE_REGEXP = "[\\w|\\-]+";
	public static final String NAMESPACE_VALIDATION_MESSAGE = "can only contain A-Z, a-z, 0-9, - and _";
	public static final String SENT_BY_HEADER = "X-Sent-By";
	public static final String UNKNOWN = "UNKNOWN";

	public static final String EXTERNAL_TAG_KEY_CASE_ID = "CaseId";
	public static final String EXTERNAL_TAG_KEY_NOTE_ID = "NoteId";
	public static final String EXTERNAL_TAG_KEY_EXECUTED_BY = "ExecutedBy";
	public static final String EXTERNAL_TAG_KEY_PREVIOUS_REVISION = "PreviousRevision";
	public static final String EXTERNAL_TAG_KEY_PREVIOUS_VERSION = "PreviousVersion";
	public static final String EXTERNAL_TAG_KEY_CURRENT_REVISION = "CurrentRevision";
	public static final String EXTERNAL_TAG_KEY_CURRENT_VERSION = "CurrentVersion";

	public static final String ERRAND_STATUS_SOLVED = "SOLVED";
	public static final String ERRAND_STATUS_ONGOING = "ONGOING";
}

package se.sundsvall.supportmanagement.integration.db.search;

import java.util.List;

/**
 * The names of the fields of the errand index that anything but the index itself refers to.
 * <p>
 * The entity mapping declares the fields under these names,
 * {@link se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField}
 * and {@link se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource} say which of them carry a
 * field or a resource, and the search reads the rest of the index from its descriptor. Naming a field here and nowhere
 * else is what lets a rename be caught by the compiler, and what the index model checks against the index at startup.
 * A name ending in a dot, made with {@link #under}, stands for every field below an object.
 */
public final class ErrandIndex {

	/** The index. */
	public static final String NAME = "errand";

	// Fields of the errand itself
	public static final String MUNICIPALITY_ID = "municipalityId";
	public static final String NAMESPACE = "namespace";
	public static final String ERRAND_NUMBER = "errandNumber";
	public static final String TITLE = "title";
	public static final String TITLE_SORT = "title_sort";
	public static final String STATUS = "status";
	public static final String RESOLUTION = "resolution";
	public static final String CHANNEL = "channel";
	public static final String CREATED = "created";
	public static final String MODIFIED = "modified";
	public static final String TOUCHED = "touched";
	public static final String PRIORITY = "priority";
	public static final String DESCRIPTION = "description";
	public static final String CATEGORY = "category";
	public static final String TYPE = "type";
	public static final String REPORTER_USER_ID = "reporterUserId";
	public static final String ASSIGNED_USER_ID = "assignedUserId";
	public static final String ASSIGNED_GROUP_ID = "assignedGroupId";
	public static final String BUSINESS_RELATED = "businessRelated";
	public static final String SUSPENDED_FROM = "suspendedFrom";
	public static final String SUSPENDED_TO = "suspendedTo";
	public static final String CONTACT_REASON = "contactReason";
	public static final String CONTACT_REASON_DESCRIPTION = "contactReasonDescription";
	public static final String ESCALATION_EMAIL = "escalationEmail";
	public static final String PREVIOUS_STATUS = "previousStatus";

	// Objects on the errand
	public static final String LABELS = "labels";
	public static final String ACCESS_LABELS = "accessLabels";
	public static final String EXTERNAL_TAGS = "externalTags";
	public static final String STAKEHOLDERS = "stakeholders";
	public static final String PARAMETERS = "parameters";
	public static final String JSON_PARAMETERS = "jsonParameters";
	public static final String JSON_PARAMETERS_TEXT = "jsonParametersText";
	public static final String ATTACHMENTS = "attachments";
	public static final String PHASES = "phases";
	public static final String PHASE = "phase";
	public static final String MEASURES = "measures";
	public static final String DECISIONS = "decisions";
	public static final String STATEMENTS = "statements";
	public static final String INVESTIGATIONS = "investigations";
	public static final String COMMUNICATIONS = "communications";

	// Fields within objects that the search names on its own
	public static final String METADATA_LABEL_ID = "metadataLabelId";
	public static final String ACCESS_LABEL_ID = ACCESS_LABELS + "." + METADATA_LABEL_ID;
	public static final String EXTERNAL_TAG_VALUE = EXTERNAL_TAGS + ".value";
	public static final String STAKEHOLDER_EXTERNAL_ID = STAKEHOLDERS + ".externalId";

	/**
	 * The keyword fields a word without a field is looked for in besides the text: the identifiers a user is likely to
	 * paste into a search box.
	 */
	public static final List<String> IDENTIFIER_FIELDS = List.of(ERRAND_NUMBER, EXTERNAL_TAG_VALUE, STAKEHOLDER_EXTERNAL_ID);

	private ErrandIndex() {}

	/** The start of the names of every field below sent in object. */
	public static String under(final String object) {
		return object + ".";
	}
}

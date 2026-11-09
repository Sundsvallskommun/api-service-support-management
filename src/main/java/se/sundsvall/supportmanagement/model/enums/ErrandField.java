package se.sundsvall.supportmanagement.model.enums;

/**
 * Fields of an errand that a role based mapping may emit. Keyed fields are the errand collections whose elements carry
 * a
 * key, which allows a namespace to expose individual entries instead of the whole collection.
 * <p>
 * Each constant carries the property it names on the errand, which is what the API reports in place of the constant
 * itself.
 */
public enum ErrandField {

	ID("id", false, null),
	ERRAND_NUMBER("errandNumber", false, null),
	TITLE("title", false, null),
	STATUS("status", false, null),
	LIFECYCLE("lifecycle", false, null),
	RESOLUTION("resolution", false, null),
	CHANNEL("channel", false, null),
	CREATED("created", false, null),
	MODIFIED("modified", false, null),
	TOUCHED("touched", false, null),
	DELETED("deleted", false, null),
	DELETED_BY("deletedBy", false, null),
	PRIORITY("priority", false, null),
	DESCRIPTION("description", false, null),
	CLASSIFICATION("classification", false, null),
	REPORTER_USER_ID("reporterUserId", false, null),
	ASSIGNED_USER_ID("assignedUserId", false, null),
	ASSIGNED_GROUP_ID("assignedGroupId", false, null),
	BUSINESS_RELATED("businessRelated", false, null),
	SUSPENSION("suspension", false, null),
	CONTACT_REASON("contactReason", false, null),
	CONTACT_REASON_DESCRIPTION("contactReasonDescription", false, null),
	ESCALATION_EMAIL("escalationEmail", false, null),
	LABELS("labels", false, null),
	STAKEHOLDERS("stakeholders", false, null),
	MEASURES("measures", false, null),
	ACTIVE_NOTIFICATIONS("activeNotifications", false, null),
	PHASES("phases", false, null),
	ACTIONS("actions", false, null),
	PROCESS("process", false, null),
	VERSION("version", false, null),
	PARAMETERS("parameters", true, ProtectedResource.PARAMETER),
	JSON_PARAMETERS("jsonParameters", true, ProtectedResource.JSON_PARAMETER),
	EXTERNAL_TAGS("externalTags", true, null);

	private final String propertyName;
	private final boolean keyed;
	private final ProtectedResource writeResource;

	ErrandField(final String propertyName, final boolean keyed, final ProtectedResource writeResource) {
		this.propertyName = propertyName;
		this.keyed = keyed;
		this.writeResource = writeResource;
	}

	/**
	 * The property this field names on the errand, as it is written in the payload.
	 */
	public String getPropertyName() {
		return propertyName;
	}

	public boolean isKeyed() {
		return keyed;
	}

	/**
	 * The resource a write to this field is guarded on where it has an endpoint of its own, null for the fields only
	 * ever written through the errand. Such a field is writable to a caller holding either the errand or that resource at
	 * read/write.
	 */
	public ProtectedResource getWriteResource() {
		return writeResource;
	}

}

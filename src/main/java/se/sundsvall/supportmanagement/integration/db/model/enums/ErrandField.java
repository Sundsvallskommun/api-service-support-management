package se.sundsvall.supportmanagement.integration.db.model.enums;

/**
 * Fields of an errand that a role based mapping may emit. Keyed fields are the errand collections whose elements carry
 * a
 * key, which allows a namespace to expose individual entries instead of the whole collection.
 * <p>
 * Each constant carries the property it names on the errand, which is what the API reports rather than the constant
 * itself. A client matches the answer against the payload it is rendering, and adding a field here then leaves the
 * published contract alone instead of widening an enum in it.
 */
public enum ErrandField {

	ID("id", false),
	ERRAND_NUMBER("errandNumber", false),
	TITLE("title", false),
	STATUS("status", false),
	RESOLUTION("resolution", false),
	CHANNEL("channel", false),
	CREATED("created", false),
	MODIFIED("modified", false),
	TOUCHED("touched", false),
	PRIORITY("priority", false),
	DESCRIPTION("description", false),
	CLASSIFICATION("classification", false),
	REPORTER_USER_ID("reporterUserId", false),
	ASSIGNED_USER_ID("assignedUserId", false),
	ASSIGNED_GROUP_ID("assignedGroupId", false),
	BUSINESS_RELATED("businessRelated", false),
	SUSPENSION("suspension", false),
	CONTACT_REASON("contactReason", false),
	CONTACT_REASON_DESCRIPTION("contactReasonDescription", false),
	ESCALATION_EMAIL("escalationEmail", false),
	LABELS("labels", false),
	STAKEHOLDERS("stakeholders", false),
	MEASURES("measures", false),
	ACTIVE_NOTIFICATIONS("activeNotifications", false),
	PHASES("phases", false),
	ACTIONS("actions", false),
	PROCESS("process", false),
	VERSION("version", false),
	PARAMETERS("parameters", true, ProtectedResource.PARAMETER),
	JSON_PARAMETERS("jsonParameters", true, ProtectedResource.JSON_PARAMETER),
	EXTERNAL_TAGS("externalTags", true);

	private final String propertyName;
	private final boolean keyed;
	private final ProtectedResource writeResource;

	ErrandField(final String propertyName, final boolean keyed) {
		this(propertyName, keyed, null);
	}

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
	 * ever written through the errand. What the caller may do with such a field follows that resource rather than the
	 * errand, since that is what the endpoint accepting the write is guarded on.
	 */
	public ProtectedResource getWriteResource() {
		return writeResource;
	}
}

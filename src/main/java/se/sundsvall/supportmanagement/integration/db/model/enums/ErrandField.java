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
	VERSION("version", false),
	PARAMETERS("parameters", true),
	JSON_PARAMETERS("jsonParameters", true),
	EXTERNAL_TAGS("externalTags", true);

	private final String propertyName;
	private final boolean keyed;

	ErrandField(final String propertyName, final boolean keyed) {
		this.propertyName = propertyName;
		this.keyed = keyed;
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
}

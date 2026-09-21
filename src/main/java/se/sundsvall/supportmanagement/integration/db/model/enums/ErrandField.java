package se.sundsvall.supportmanagement.integration.db.model.enums;

import java.util.List;

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

	ID("id", false, null),
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
	CLASSIFICATION("classification", false, null, "category", "type"),
	REPORTER_USER_ID("reporterUserId", false),
	ASSIGNED_USER_ID("assignedUserId", false),
	ASSIGNED_GROUP_ID("assignedGroupId", false),
	BUSINESS_RELATED("businessRelated", false),
	SUSPENSION("suspension", false, null, "suspendedFrom", "suspendedTo"),
	CONTACT_REASON("contactReason", false, null, "contactReason."),
	CONTACT_REASON_DESCRIPTION("contactReasonDescription", false),
	ESCALATION_EMAIL("escalationEmail", false),
	LABELS("labels", false, null, "labels."),
	STAKEHOLDERS("stakeholders", false, null, "stakeholders."),
	MEASURES("measures", false, null, "measures."),
	ACTIVE_NOTIFICATIONS("activeNotifications", false, null),
	PHASES("phases", false, null, "phases."),
	ACTIONS("actions", false, null),
	VERSION("version", false, null),
	PARAMETERS("parameters", true, ProtectedResource.PARAMETER, "parameters."),
	JSON_PARAMETERS("jsonParameters", true, ProtectedResource.JSON_PARAMETER, "jsonParameters.", "jsonParametersText"),
	EXTERNAL_TAGS("externalTags", true, null, "externalTags.");

	private final String propertyName;
	private final boolean keyed;
	private final ProtectedResource writeResource;
	private final List<String> searchFields;

	/**
	 * A field the search index holds under the name of the property.
	 */
	ErrandField(final String propertyName, final boolean keyed) {
		this(propertyName, keyed, null, propertyName);
	}

	/**
	 * @param searchFields the fields of the search index that hold this field, by their names or, for a name ending in a
	 *                     dot, by the start of their names. None for a field the index does not hold.
	 */
	ErrandField(final String propertyName, final boolean keyed, final ProtectedResource writeResource, final String... searchFields) {
		this.propertyName = propertyName;
		this.keyed = keyed;
		this.writeResource = writeResource;
		this.searchFields = List.of(searchFields);
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

	/**
	 * The fields of the search index that hold this field. What a role keeps from a user of the field, it keeps from
	 * them in a search.
	 */
	public List<String> getSearchFields() {
		return searchFields;
	}
}

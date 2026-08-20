package se.sundsvall.supportmanagement.integration.db.model.enums;

/**
 * Fields of an errand that a role based mapping may emit. Keyed fields are the errand collections whose elements carry
 * a
 * key, which allows a namespace to expose individual entries instead of the whole collection.
 */
public enum ErrandField {

	ID(false),
	ERRAND_NUMBER(false),
	TITLE(false),
	STATUS(false),
	RESOLUTION(false),
	CHANNEL(false),
	CREATED(false),
	MODIFIED(false),
	TOUCHED(false),
	PRIORITY(false),
	DESCRIPTION(false),
	CLASSIFICATION(false),
	REPORTER_USER_ID(false),
	ASSIGNED_USER_ID(false),
	ASSIGNED_GROUP_ID(false),
	BUSINESS_RELATED(false),
	SUSPENSION(false),
	CONTACT_REASON(false),
	CONTACT_REASON_DESCRIPTION(false),
	ESCALATION_EMAIL(false),
	LABELS(false),
	STAKEHOLDERS(false),
	ACTIVE_NOTIFICATIONS(false),
	VERSION(false),
	PARAMETERS(true),
	JSON_PARAMETERS(true),
	EXTERNAL_TAGS(true);

	private final boolean keyed;

	ErrandField(final boolean keyed) {
		this.keyed = keyed;
	}

	public boolean isKeyed() {
		return keyed;
	}
}

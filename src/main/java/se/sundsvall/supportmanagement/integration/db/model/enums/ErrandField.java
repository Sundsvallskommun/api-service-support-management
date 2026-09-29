package se.sundsvall.supportmanagement.integration.db.model.enums;

import java.util.List;
import java.util.Optional;
import se.sundsvall.supportmanagement.integration.db.search.ErrandIndex;

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

	ID("id", false, null, IndexBinding.none()),
	ERRAND_NUMBER("errandNumber", false, null, IndexBinding.of(ErrandIndex.ERRAND_NUMBER).sortedBy(ErrandIndex.ERRAND_NUMBER)),
	TITLE("title", false, null, IndexBinding.of(ErrandIndex.TITLE).sortedBy(ErrandIndex.TITLE_SORT)),
	STATUS("status", false, null, IndexBinding.of(ErrandIndex.STATUS).sortedBy(ErrandIndex.STATUS)),
	RESOLUTION("resolution", false, null, IndexBinding.of(ErrandIndex.RESOLUTION).sortedBy(ErrandIndex.RESOLUTION)),
	CHANNEL("channel", false, null, IndexBinding.of(ErrandIndex.CHANNEL).sortedBy(ErrandIndex.CHANNEL)),
	CREATED("created", false, null, IndexBinding.of(ErrandIndex.CREATED).sortedBy(ErrandIndex.CREATED)),
	MODIFIED("modified", false, null, IndexBinding.of(ErrandIndex.MODIFIED).sortedBy(ErrandIndex.MODIFIED)),
	TOUCHED("touched", false, null, IndexBinding.of(ErrandIndex.TOUCHED).sortedBy(ErrandIndex.TOUCHED)),
	PRIORITY("priority", false, null, IndexBinding.of(ErrandIndex.PRIORITY).sortedBy(ErrandIndex.PRIORITY)),
	DESCRIPTION("description", false, null, IndexBinding.of(ErrandIndex.DESCRIPTION)),
	CLASSIFICATION("classification", false, null, IndexBinding.of(ErrandIndex.CATEGORY, ErrandIndex.TYPE)
		.sortedBy("category", ErrandIndex.CATEGORY)
		.sortedBy("type", ErrandIndex.TYPE)),
	REPORTER_USER_ID("reporterUserId", false, null, IndexBinding.of(ErrandIndex.REPORTER_USER_ID).sortedBy(ErrandIndex.REPORTER_USER_ID)),
	ASSIGNED_USER_ID("assignedUserId", false, null, IndexBinding.of(ErrandIndex.ASSIGNED_USER_ID).sortedBy(ErrandIndex.ASSIGNED_USER_ID)),
	ASSIGNED_GROUP_ID("assignedGroupId", false, null, IndexBinding.of(ErrandIndex.ASSIGNED_GROUP_ID).sortedBy(ErrandIndex.ASSIGNED_GROUP_ID)),
	BUSINESS_RELATED("businessRelated", false, null, IndexBinding.of(ErrandIndex.BUSINESS_RELATED)),
	SUSPENSION("suspension", false, null, IndexBinding.of(ErrandIndex.SUSPENDED_FROM, ErrandIndex.SUSPENDED_TO)
		.sortedBy("suspendedFrom", ErrandIndex.SUSPENDED_FROM)
		.sortedBy("suspendedTo", ErrandIndex.SUSPENDED_TO)),
	CONTACT_REASON("contactReason", false, null, IndexBinding.of(ErrandIndex.under(ErrandIndex.CONTACT_REASON))),
	CONTACT_REASON_DESCRIPTION("contactReasonDescription", false, null, IndexBinding.of(ErrandIndex.CONTACT_REASON_DESCRIPTION)),
	ESCALATION_EMAIL("escalationEmail", false, null, IndexBinding.of(ErrandIndex.ESCALATION_EMAIL)),
	LABELS("labels", false, null, IndexBinding.of(ErrandIndex.under(ErrandIndex.LABELS))),
	STAKEHOLDERS("stakeholders", false, null, IndexBinding.of(ErrandIndex.under(ErrandIndex.STAKEHOLDERS))),
	MEASURES("measures", false, null, IndexBinding.of(ErrandIndex.under(ErrandIndex.MEASURES))),
	ACTIVE_NOTIFICATIONS("activeNotifications", false, null, IndexBinding.none()),
	PHASES("phases", false, null, IndexBinding.of(ErrandIndex.under(ErrandIndex.PHASES))),
	ACTIONS("actions", false, null, IndexBinding.none()),
	VERSION("version", false, null, IndexBinding.none()),
	PARAMETERS("parameters", true, ProtectedResource.PARAMETER, IndexBinding.of(ErrandIndex.under(ErrandIndex.PARAMETERS))),
	JSON_PARAMETERS("jsonParameters", true, ProtectedResource.JSON_PARAMETER, IndexBinding.of(ErrandIndex.under(ErrandIndex.JSON_PARAMETERS), ErrandIndex.JSON_PARAMETERS_TEXT).withKeysAsPaths()),
	EXTERNAL_TAGS("externalTags", true, null, IndexBinding.of(ErrandIndex.under(ErrandIndex.EXTERNAL_TAGS)));

	private final String propertyName;
	private final boolean keyed;
	private final ProtectedResource writeResource;
	private final IndexBinding index;

	ErrandField(final String propertyName, final boolean keyed, final ProtectedResource writeResource, final IndexBinding index) {
		this.propertyName = propertyName;
		this.keyed = keyed;
		this.writeResource = writeResource;
		this.index = index;
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
	 * How the search index holds this field. What a role keeps from a user of the field, it keeps from them in a search.
	 */
	public IndexBinding getIndex() {
		return index;
	}

	/** The index fields that hold this field, see {@link IndexBinding#fields()}. */
	public List<String> getSearchFields() {
		return index.fields();
	}

	/**
	 * The index field an ordering by sent in property sorts on, empty when the property is not one of this field or
	 * cannot be sorted on.
	 */
	public Optional<String> getSortField(final String property) {
		final var key = propertyName.equals(property) ? IndexBinding.OWN_PROPERTY : property;
		return Optional.ofNullable(index.sorts().get(key));
	}

	/** The properties an ordering may name for this field. */
	public List<String> getSortableProperties() {
		return index.sorts().keySet().stream()
			.map(key -> IndexBinding.OWN_PROPERTY.equals(key) ? propertyName : key)
			.toList();
	}
}

package se.sundsvall.supportmanagement.integration.db.model.enums;

import java.util.List;
import se.sundsvall.supportmanagement.integration.db.search.ErrandIndex;

/**
 * Resources that can be guarded by access control. Each constant carries a hierarchical path which access patterns are
 * matched against, allowing a single pattern to cover a whole subtree, e.g. "errand/communication/**" for the
 * communication of an errand, "metadata/**" for everything the namespace is configured with, or "**" for all of it.
 * <p>
 * Errand resources are guarded per errand, combining the labels of the user with these. The remaining resources belong
 * to the namespace rather than to any errand, and are guarded on these alone.
 */
public enum ProtectedResource {

	ERRAND("errand"),
	ATTACHMENT("errand/attachment", ErrandIndex.under(ErrandIndex.ATTACHMENTS)),
	COMMUNICATION("errand/communication", ErrandIndex.under(ErrandIndex.COMMUNICATIONS)),
	COMMUNICATION_ATTACHMENT("errand/communication/attachment"),
	CONVERSATION("errand/conversation"),
	CONVERSATION_MESSAGE("errand/conversation/message"),
	CONVERSATION_ATTACHMENT("errand/conversation/attachment"),
	EVENT("errand/event"),
	NOTE("errand/note"),
	NOTE_REVISION("errand/note/revision"),
	PARAMETER("errand/parameter", ErrandIndex.under(ErrandIndex.PARAMETERS)),
	JSON_PARAMETER("errand/json-parameter", ErrandIndex.under(ErrandIndex.JSON_PARAMETERS), ErrandIndex.JSON_PARAMETERS_TEXT),
	MEASURE("errand/measure", ErrandIndex.under(ErrandIndex.MEASURES)),
	STATEMENT("errand/statement", ErrandIndex.under(ErrandIndex.STATEMENTS)),
	INVESTIGATION("errand/investigation", ErrandIndex.under(ErrandIndex.INVESTIGATIONS)),
	DECISION("errand/decision", ErrandIndex.under(ErrandIndex.DECISIONS)),
	NOTIFICATION("errand/notification"),
	REVISION("errand/revision"),
	TIME_MEASURE("errand/time-measure"),

	NAMESPACE_CONFIG("namespace-config"),
	EMAIL_INTEGRATION_CONFIG("email-integration-config"),
	MESSAGE_EXCHANGE_INTEGRATION_CONFIG("messageexchange-integration-config"),
	METADATA_ATTACHMENT_PURPOSE("metadata/attachment-purpose"),
	METADATA_CATEGORY("metadata/category"),
	METADATA_CONTACT_REASON("metadata/contact-reason"),
	METADATA_DECISION_OUTCOME("metadata/decision-outcome"),
	METADATA_EXTERNAL_ID_TYPE("metadata/external-id-type"),
	METADATA_LABEL("metadata/label"),
	METADATA_MEASURE_TYPE("metadata/measure-type"),
	METADATA_PHASE("metadata/phase"),
	METADATA_ROLE("metadata/role"),
	METADATA_STATEMENT_OUTCOME("metadata/statement-outcome"),
	METADATA_STATUS("metadata/status"),
	SUBSCRIBER("subscriber"),
	SUBSCRIPTION("subscriber/subscription"),
	SUBSCRIBER_NOTIFICATION("subscriber-notification");

	private final String path;
	private final List<String> searchFields;

	ProtectedResource(final String path, final String... searchFields) {
		this.path = path;
		this.searchFields = List.of(searchFields);
	}

	public String getPath() {
		return path;
	}

	/**
	 * The fields of the search index that hold this resource, by their names or, for a name ending in a dot, by the start
	 * of their names. Empty for a resource the index does not hold, which is every resource not belonging to an errand
	 * and the errand itself, whose own fields are named by {@link ErrandField}. What is guarded on the resource is
	 * guarded on these in a search.
	 */
	public List<String> getSearchFields() {
		return searchFields;
	}

	/**
	 * Signals if the resource belongs to an errand rather than to the namespace itself, which is what separates the
	 * resources guarded per errand from those guarded on the access mapper alone. Kept next to the paths, since it is the
	 * paths it reads.
	 */
	public boolean isErrandScoped() {
		return ERRAND.path.equals(path) || path.startsWith(ERRAND.path + "/");
	}
}

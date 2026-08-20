package se.sundsvall.supportmanagement.integration.db.model.enums;

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
	ATTACHMENT("errand/attachment"),
	COMMUNICATION("errand/communication"),
	COMMUNICATION_ATTACHMENT("errand/communication/attachment"),
	CONVERSATION("errand/conversation"),
	CONVERSATION_MESSAGE("errand/conversation/message"),
	CONVERSATION_ATTACHMENT("errand/conversation/attachment"),
	EVENT("errand/event"),
	NOTE("errand/note"),
	NOTE_REVISION("errand/note/revision"),
	PARAMETER("errand/parameter"),
	JSON_PARAMETER("errand/json-parameter"),
	NOTIFICATION("errand/notification"),
	REVISION("errand/revision"),
	TIME_MEASURE("errand/time-measure"),

	NAMESPACE_CONFIG("namespace-config"),
	EMAIL_INTEGRATION_CONFIG("email-integration-config"),
	MESSAGE_EXCHANGE_INTEGRATION_CONFIG("messageexchange-integration-config"),
	METADATA_CATEGORY("metadata/category"),
	METADATA_CONTACT_REASON("metadata/contact-reason"),
	METADATA_EXTERNAL_ID_TYPE("metadata/external-id-type"),
	METADATA_LABEL("metadata/label"),
	METADATA_PHASE("metadata/phase"),
	METADATA_ROLE("metadata/role"),
	METADATA_STATUS("metadata/status"),
	SUBSCRIBER("subscriber"),
	SUBSCRIPTION("subscriber/subscription"),
	SUBSCRIBER_NOTIFICATION("subscriber-notification");

	private final String path;

	ProtectedResource(final String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}

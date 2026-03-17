package se.sundsvall.supportmanagement.integration.elasticsearch;

/**
 * Event published after an errand's JSON parameters have been persisted or updated in the database.
 * Consumed by {@link JsonParameterSyncEventListener} to sync data to Elasticsearch.
 */
public record JsonParameterSyncEvent(String errandId, String namespace, String municipalityId, Type type) {

	public enum Type {
		UPSERT,
		DELETE
	}

	public static JsonParameterSyncEvent upsert(final String errandId, final String namespace, final String municipalityId) {
		return new JsonParameterSyncEvent(errandId, namespace, municipalityId, Type.UPSERT);
	}

	public static JsonParameterSyncEvent delete(final String errandId, final String namespace, final String municipalityId) {
		return new JsonParameterSyncEvent(errandId, namespace, municipalityId, Type.DELETE);
	}
}

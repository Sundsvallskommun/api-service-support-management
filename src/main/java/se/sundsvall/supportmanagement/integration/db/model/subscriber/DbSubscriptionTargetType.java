package se.sundsvall.supportmanagement.integration.db.model.subscriber;

/**
 * Database-side equivalent of {@link se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTargetType}.
 * Prefixed with {@code Db} to disambiguate from the API enum in code that references both.
 */
public enum DbSubscriptionTargetType {

	ERRAND,
	NAMESPACE
}

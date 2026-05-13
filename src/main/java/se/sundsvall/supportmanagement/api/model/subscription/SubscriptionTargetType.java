package se.sundsvall.supportmanagement.api.model.subscription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What kind of object a subscription targets")
public enum SubscriptionTargetType {

	/** Subscription targets a specific errand. {@code SubscriptionTarget.id} must be the errand id. */
	ERRAND,

	/**
	 * Subscription targets every event in the (municipality, namespace) tuple. {@code SubscriptionTarget.id} is ignored.
	 */
	NAMESPACE
}

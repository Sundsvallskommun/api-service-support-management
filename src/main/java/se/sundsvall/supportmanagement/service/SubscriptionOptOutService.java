package se.sundsvall.supportmanagement.service;

import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.SubscriptionOptOutRepository;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionOptOutEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionProfileEntity;

import static java.util.stream.Collectors.toSet;
import static se.sundsvall.supportmanagement.integration.db.model.subscriber.DbSubscriptionTargetType.NAMESPACE;

/**
 * Keeps track of the principals who have left a subscription profile, so that the members sync respects their choice.
 * <p>
 * A namespace subscription to a profile is what membership of the profile consists of. Subscriptions to a profile for
 * a single errand are not memberships, and leaving them is not recorded.
 */
@Service
public class SubscriptionOptOutService {

	private final SubscriptionOptOutRepository subscriptionOptOutRepository;

	public SubscriptionOptOutService(final SubscriptionOptOutRepository subscriptionOptOutRepository) {
		this.subscriptionOptOutRepository = subscriptionOptOutRepository;
	}

	public static boolean isProfileMembership(final SubscriptionEntity subscription) {
		return subscription.getProfile() != null && subscription.getTargetType() == NAMESPACE;
	}

	/**
	 * Records that the principal behind the subscription left its profile, unless the subscription is no membership or
	 * that is already recorded.
	 */
	@Transactional
	public void recordOptOut(final SubscriptionEntity subscription) {
		if (!isProfileMembership(subscription)) {
			return;
		}
		final var profile = subscription.getProfile();
		final var identifier = subscription.getSubscriber().getIdentifier();
		if (!subscriptionOptOutRepository.existsByProfileIdAndIdentifierTypeAndIdentifierValue(profile.getId(), identifier.getType(), identifier.getValue())) {
			subscriptionOptOutRepository.save(SubscriptionOptOutEntity.create()
				.withProfile(profile)
				.withIdentifier(IdentifierEmbeddable.create().withType(identifier.getType()).withValue(identifier.getValue())));
		}
	}

	@Transactional(readOnly = true)
	public boolean isOptedOut(final String profileId, final IdentifierEmbeddable identifier) {
		return subscriptionOptOutRepository.existsByProfileIdAndIdentifierTypeAndIdentifierValue(profileId, identifier.getType(), identifier.getValue());
	}

	/**
	 * Clears a recorded opt-out, as the principal has chosen the profile again.
	 */
	@Transactional
	public void clearOptOut(final SubscriptionProfileEntity profile, final IdentifierEmbeddable identifier) {
		subscriptionOptOutRepository.deleteByProfileIdAndIdentifierTypeAndIdentifierValue(profile.getId(), identifier.getType(), identifier.getValue());
	}

	/**
	 * The principals who left the profile, as keys made by {@link #keyOf(String, String)}.
	 */
	@Transactional(readOnly = true)
	public Set<String> findOptedOutKeys(final String profileId) {
		return subscriptionOptOutRepository.findAllByProfileId(profileId).stream()
			.map(optOut -> keyOf(optOut.getIdentifier().getType(), optOut.getIdentifier().getValue()))
			.collect(toSet());
	}

	/**
	 * A key telling principals apart regardless of case, as AD accounts are.
	 */
	public static String keyOf(final String type, final String value) {
		return (type + ":" + value).toLowerCase(Locale.ROOT);
	}
}

package se.sundsvall.supportmanagement.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.integration.db.SubscriptionOptOutRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriptionRepository;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;
import se.sundsvall.supportmanagement.service.mapper.IdentifierEmbeddableMapper;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static se.sundsvall.supportmanagement.integration.db.model.subscriber.DbSubscriptionTargetType.NAMESPACE;

/**
 * Keeps the members of a subscription profile in line with a list kept elsewhere, typically by a job mapping roles to
 * profiles. A member is a subscriber holding a namespace subscription to the profile.
 */
@Service
public class SubscriptionProfileMemberService {

	private final SubscriptionProfileService subscriptionProfileService;
	private final SubscriberService subscriberService;
	private final SubscriptionRepository subscriptionRepository;
	private final SubscriptionOptOutRepository subscriptionOptOutRepository;

	public SubscriptionProfileMemberService(
		final SubscriptionProfileService subscriptionProfileService,
		final SubscriberService subscriberService,
		final SubscriptionRepository subscriptionRepository,
		final SubscriptionOptOutRepository subscriptionOptOutRepository) {
		this.subscriptionProfileService = subscriptionProfileService;
		this.subscriberService = subscriberService;
		this.subscriptionRepository = subscriptionRepository;
		this.subscriptionOptOutRepository = subscriptionOptOutRepository;
	}

	@Transactional(readOnly = true)
	public List<Identifier> findMembers(final String municipalityId, final String namespace, final String profileId) {
		final var profile = subscriptionProfileService.findEntity(municipalityId, namespace, profileId);
		return subscriptionRepository.findAllByProfileIdAndTargetType(profile.getId(), NAMESPACE).stream()
			.map(subscription -> IdentifierEmbeddableMapper.toIdentifier(subscription.getSubscriber().getIdentifier()))
			.toList();
	}

	/**
	 * Makes the given principals the members of the profile. Principals not yet members are subscribed, creating a
	 * subscriber for them when they have none, while members missing from the list lose their subscription to the
	 * profile. A principal who left the profile of their own accord is not subscribed again, which is why removing a
	 * member here records no opt-out: only the member themselves leaving does.
	 * <p>
	 * Identifiers are compared without regard to case, as AD accounts are.
	 */
	@Transactional
	public void syncMembers(final String municipalityId, final String namespace, final String profileId, final List<Identifier> members) {
		final var profile = subscriptionProfileService.findEntity(municipalityId, namespace, profileId);

		final var wanted = members.stream()
			.collect(toMap(SubscriptionProfileMemberService::keyOf, Function.identity(), (first, _) -> first, LinkedHashMap::new));
		final var current = subscriptionRepository.findAllByProfileIdAndTargetType(profile.getId(), NAMESPACE).stream()
			.collect(toMap(subscription -> keyOf(subscription.getSubscriber().getIdentifier()), Function.identity(), (first, _) -> first, LinkedHashMap::new));
		final var optedOut = subscriptionOptOutRepository.findAllByProfileId(profile.getId()).stream()
			.map(optOut -> optOut.getSubscriber().getId())
			.collect(toSet());

		current.entrySet().stream()
			.filter(entry -> !wanted.containsKey(entry.getKey()))
			.map(Map.Entry::getValue)
			.forEach(subscriptionRepository::delete);

		wanted.entrySet().stream()
			.filter(entry -> !current.containsKey(entry.getKey()))
			.map(Map.Entry::getValue)
			.map(member -> subscriberService.findOrCreateSubscriber(municipalityId, namespace, member.getType(), member.getValue()))
			.filter(subscriber -> !optedOut.contains(subscriber.getId()))
			.forEach(subscriber -> subscriptionRepository.save(SubscriptionEntity.create()
				.withSubscriber(subscriber)
				.withTargetType(NAMESPACE)
				.withProfile(profile)
				.withCreatedBy(IdentifierEmbeddableMapper.fromExecutingUser(se.sundsvall.dept44.support.Identifier.get()))));
	}

	private static String keyOf(final Identifier identifier) {
		return key(identifier.getType(), identifier.getValue());
	}

	private static String keyOf(final IdentifierEmbeddable identifier) {
		return key(identifier.getType(), identifier.getValue());
	}

	private static String key(final String type, final String value) {
		return (type + ":" + value).toLowerCase(Locale.ROOT);
	}
}

package se.sundsvall.supportmanagement.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.integration.db.SubscriptionRepository;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;
import se.sundsvall.supportmanagement.service.mapper.IdentifierEmbeddableMapper;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static se.sundsvall.supportmanagement.integration.db.model.subscriber.DbSubscriptionTargetType.NAMESPACE;
import static se.sundsvall.supportmanagement.service.SubscriptionOptOutService.keyOf;

/**
 * Keeps the members of a subscription profile in line with a list kept elsewhere, typically by a job mapping roles to
 * profiles. A member is a subscriber holding a namespace subscription to the profile.
 */
@Service
public class SubscriptionProfileMemberService {

	private final SubscriptionProfileService subscriptionProfileService;
	private final SubscriberService subscriberService;
	private final SubscriptionRepository subscriptionRepository;
	private final SubscriptionOptOutService subscriptionOptOutService;

	public SubscriptionProfileMemberService(
		final SubscriptionProfileService subscriptionProfileService,
		final SubscriberService subscriberService,
		final SubscriptionRepository subscriptionRepository,
		final SubscriptionOptOutService subscriptionOptOutService) {
		this.subscriptionProfileService = subscriptionProfileService;
		this.subscriberService = subscriberService;
		this.subscriptionRepository = subscriptionRepository;
		this.subscriptionOptOutService = subscriptionOptOutService;
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
	 * profile, on every subscriber of theirs holding one. A principal who left the profile of their own accord is not
	 * subscribed again, which is why removing a member here records no opt-out: only the member themselves leaving does.
	 * <p>
	 * Principals are told apart by their identifier, compared without regard to case as AD accounts are, rather than by
	 * subscriber, since one principal may hold several.
	 */
	@Transactional
	public void syncMembers(final String municipalityId, final String namespace, final String profileId, final List<Identifier> members) {
		final var profile = subscriptionProfileService.findEntity(municipalityId, namespace, profileId);

		final var wanted = members.stream()
			.collect(toMap(member -> keyOf(member.getType(), member.getValue()), Function.identity(), (first, _) -> first, LinkedHashMap::new));
		final var current = subscriptionRepository.findAllByProfileIdAndTargetType(profile.getId(), NAMESPACE).stream()
			.collect(groupingBy(subscription -> keyOf(subscription.getSubscriber().getIdentifier().getType(), subscription.getSubscriber().getIdentifier().getValue()),
				LinkedHashMap::new, toList()));
		final var optedOut = subscriptionOptOutService.findOptedOutKeys(profile.getId());

		current.entrySet().stream()
			.filter(entry -> !wanted.containsKey(entry.getKey()))
			.flatMap(entry -> entry.getValue().stream())
			.forEach(subscriptionRepository::delete);

		wanted.entrySet().stream()
			.filter(entry -> !current.containsKey(entry.getKey()))
			.filter(entry -> !optedOut.contains(entry.getKey()))
			.map(Map.Entry::getValue)
			.map(member -> subscriberService.findOrCreateSubscriber(municipalityId, namespace, member.getType(), member.getValue()))
			.forEach(subscriber -> subscriptionRepository.save(SubscriptionEntity.create()
				.withSubscriber(subscriber)
				.withTargetType(NAMESPACE)
				.withProfile(profile)
				.withCreatedBy(IdentifierEmbeddableMapper.fromExecutingUser(se.sundsvall.dept44.support.Identifier.get()))));
	}
}

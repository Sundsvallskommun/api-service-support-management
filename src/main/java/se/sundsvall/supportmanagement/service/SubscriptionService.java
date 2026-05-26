package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.subscription.Subscription;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTarget;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTargetType;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriptionRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;
import se.sundsvall.supportmanagement.service.mapper.SubscriberMapper;
import se.sundsvall.supportmanagement.service.mapper.SubscriptionMapper;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SubscriptionService {

	private static final String SUBSCRIPTION_NOT_FOUND = "Subscription with id:'%s' not found for subscriber with id:'%s' in namespace:'%s' for municipality with id:'%s'";
	private static final String ERRAND_NOT_FOUND = "Errand with id:'%s' not found in namespace:'%s' for municipality with id:'%s'";
	private static final String TARGET_ID_REQUIRED_FOR_ERRAND = "Subscription target id is required when target type is ERRAND";
	private static final String TARGET_ID_NOT_ALLOWED_FOR_NAMESPACE = "Subscription target id must be null when target type is NAMESPACE";
	private static final String DUPLICATE_ERRAND_SUBSCRIPTION = "Subscription for errand:'%s' already exists for subscriber with id:'%s'";
	private static final String DUPLICATE_NAMESPACE_SUBSCRIPTION = "Namespace subscription already exists for subscriber with id:'%s'";

	private final SubscriberService subscriberService;
	private final SubscriptionRepository subscriptionRepository;
	private final ErrandsRepository errandsRepository;

	public SubscriptionService(
		final SubscriberService subscriberService,
		final SubscriptionRepository subscriptionRepository,
		final ErrandsRepository errandsRepository) {
		this.subscriberService = subscriberService;
		this.subscriptionRepository = subscriptionRepository;
		this.errandsRepository = errandsRepository;
	}

	@Transactional(readOnly = true)
	public List<Subscription> findSubscriptions(final String municipalityId, final String namespace, final String subscriberId) {
		subscriberService.findEntity(municipalityId, namespace, subscriberId);
		return subscriptionRepository.findAllBySubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId(subscriberId, namespace, municipalityId)
			.stream()
			.map(SubscriptionMapper::toSubscription)
			.toList();
	}

	@Transactional
	public String createSubscription(final String municipalityId, final String namespace, final String subscriberId, final Subscription subscription) {
		final var subscriber = subscriberService.findEntity(municipalityId, namespace, subscriberId);
		final var target = subscription.getTarget();
		validateTarget(target);

		final var errand = resolveErrand(target, namespace, municipalityId);
		rejectDuplicate(subscriberId, target.getType(), errand);

		final var entity = SubscriptionMapper.toSubscriptionEntity(subscriber, errand, subscription)
			.withCreatedBy(SubscriberMapper.fromExecutingUser(Identifier.get()));

		return subscriptionRepository.save(entity).getId();
	}

	@Transactional
	public void deleteSubscription(final String municipalityId, final String namespace, final String subscriberId, final String subscriptionId) {
		final var entity = loadSubscriptionOrThrow(municipalityId, namespace, subscriberId, subscriptionId);
		subscriptionRepository.delete(entity);
	}

	private void validateTarget(final SubscriptionTarget target) {
		if (target.getType() == SubscriptionTargetType.ERRAND && target.getId() == null) {
			throw Problem.valueOf(BAD_REQUEST, TARGET_ID_REQUIRED_FOR_ERRAND);
		}
		if (target.getType() == SubscriptionTargetType.NAMESPACE && target.getId() != null) {
			throw Problem.valueOf(BAD_REQUEST, TARGET_ID_NOT_ALLOWED_FOR_NAMESPACE);
		}
	}

	private ErrandEntity resolveErrand(final SubscriptionTarget target, final String namespace, final String municipalityId) {
		if (target.getType() != SubscriptionTargetType.ERRAND) {
			return null;
		}
		return errandsRepository.findByIdAndNamespaceAndMunicipalityId(target.getId(), namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND.formatted(target.getId(), namespace, municipalityId)));
	}

	private void rejectDuplicate(final String subscriberId, final SubscriptionTargetType targetType, final ErrandEntity errand) {
		final var dbTargetType = SubscriptionMapper.toDbTargetType(targetType);
		if (targetType == SubscriptionTargetType.ERRAND) {
			if (subscriptionRepository.existsBySubscriberIdAndTargetTypeAndErrandId(subscriberId, dbTargetType, errand.getId())) {
				throw Problem.valueOf(CONFLICT, DUPLICATE_ERRAND_SUBSCRIPTION.formatted(errand.getId(), subscriberId));
			}
		} else if (subscriptionRepository.existsBySubscriberIdAndTargetTypeAndErrandIsNull(subscriberId, dbTargetType)) {
			throw Problem.valueOf(CONFLICT, DUPLICATE_NAMESPACE_SUBSCRIPTION.formatted(subscriberId));
		}
	}

	private SubscriptionEntity loadSubscriptionOrThrow(final String municipalityId, final String namespace, final String subscriberId, final String subscriptionId) {
		return subscriptionRepository.findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId(subscriptionId, subscriberId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, SUBSCRIPTION_NOT_FOUND.formatted(subscriptionId, subscriberId, namespace, municipalityId)));
	}
}

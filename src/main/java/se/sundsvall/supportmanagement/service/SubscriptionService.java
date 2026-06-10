package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.subscription.Subscription;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTarget;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTargetType;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriptionRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.DbSubscriptionTargetType;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;
import se.sundsvall.supportmanagement.service.mapper.IdentifierEmbeddableMapper;
import se.sundsvall.supportmanagement.service.mapper.SubscriptionMapper;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SubscriptionService {

	private static final Logger LOG = LoggerFactory.getLogger(SubscriptionService.class);

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
			.withCreatedBy(IdentifierEmbeddableMapper.fromExecutingUser(Identifier.get()));

		return persistOrThrowConflict(entity, subscriberId, target.getType(), errand).getId();
	}

	@Transactional
	public void deleteSubscription(final String municipalityId, final String namespace, final String subscriberId, final String subscriptionId) {
		final var entity = loadSubscriptionOrThrow(municipalityId, namespace, subscriberId, subscriptionId);
		subscriptionRepository.delete(entity);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handleAutoSubscribeEvent(final AutoSubscribeEvent event) {
		try {
			autoSubscribeErrandAssignee(event.errandEntity());
		} catch (final Exception e) {
			LOG.warn("Auto-subscribe failed for errand '{}' – continuing without subscription", event.errandEntity().getId(), e);
		}
	}

	@Transactional
	public void autoSubscribeErrandAssignee(final ErrandEntity errand) {
		final var assignedUserId = errand.getAssignedUserId();
		if (assignedUserId == null) {
			return;
		}
		final var subscriber = subscriberService.findOrCreateSubscriberForAssignee(
			errand.getMunicipalityId(), errand.getNamespace(), assignedUserId);
		if (!subscriptionRepository.existsBySubscriberIdAndTargetTypeAndErrandId(
			subscriber.getId(), DbSubscriptionTargetType.ERRAND, errand.getId())) {
			subscriptionRepository.save(SubscriptionEntity.create()
				.withSubscriber(subscriber)
				.withTargetType(DbSubscriptionTargetType.ERRAND)
				.withErrand(errand));
		}
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
				throw duplicateConflict(targetType, subscriberId, errand);
			}
		} else if (subscriptionRepository.existsBySubscriberIdAndTargetTypeAndErrandIsNull(subscriberId, dbTargetType)) {
			throw duplicateConflict(targetType, subscriberId, errand);
		}
	}

	// Flush eagerly so the uq_subscription_subscriber_target_errand constraint (V1_36) fires
	// inside this method, catching the TOCTOU race past rejectDuplicate. Translate to 409 instead of 500.
	private SubscriptionEntity persistOrThrowConflict(final SubscriptionEntity entity, final String subscriberId, final SubscriptionTargetType targetType, final ErrandEntity errand) {
		try {
			return subscriptionRepository.saveAndFlush(entity);
		} catch (final DataIntegrityViolationException e) {
			throw duplicateConflict(targetType, subscriberId, errand);
		}
	}

	private static se.sundsvall.dept44.problem.ThrowableProblem duplicateConflict(final SubscriptionTargetType targetType, final String subscriberId, final ErrandEntity errand) {
		if (targetType == SubscriptionTargetType.ERRAND) {
			return Problem.valueOf(CONFLICT, DUPLICATE_ERRAND_SUBSCRIPTION.formatted(errand.getId(), subscriberId));
		}
		return Problem.valueOf(CONFLICT, DUPLICATE_NAMESPACE_SUBSCRIPTION.formatted(subscriberId));
	}

	private SubscriptionEntity loadSubscriptionOrThrow(final String municipalityId, final String namespace, final String subscriberId, final String subscriptionId) {
		return subscriptionRepository.findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId(subscriptionId, subscriberId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, SUBSCRIPTION_NOT_FOUND.formatted(subscriptionId, subscriberId, namespace, municipalityId)));
	}
}

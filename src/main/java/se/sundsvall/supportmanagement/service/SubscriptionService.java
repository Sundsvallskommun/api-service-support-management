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
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;
import se.sundsvall.supportmanagement.service.mapper.IdentifierEmbeddableMapper;
import se.sundsvall.supportmanagement.service.mapper.SubscriptionMapper;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getAdUser;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.isRequestingUser;

@Service
public class SubscriptionService {

	private static final Logger LOG = LoggerFactory.getLogger(SubscriptionService.class);

	private static final String ERRAND_NOT_FOUND = "Errand with id:'%s' not found in namespace:'%s' for municipality with id:'%s'";
	private static final String SUBSCRIPTION_NOT_OWNED = "Subscriptions of subscriber '%s' not accessible by user '%s'";
	private static final String SUBSCRIPTION_NOT_FOUND = "Subscription with id:'%s' not found for subscriber with id:'%s' in namespace:'%s' for municipality with id:'%s'";
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
		verifyOwnedByRequestingUser(subscriberService.findEntity(municipalityId, namespace, subscriberId));
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
		verifyOwnedByRequestingUser(subscriberService.findEntity(municipalityId, namespace, subscriberId));
		final var entity = loadSubscriptionOrThrow(municipalityId, namespace, subscriberId, subscriptionId);
		subscriptionRepository.delete(entity);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
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

	/**
	 * A subscriber's subscriptions are their own to read and remove. Anyone may create one, since subscribing a
	 * colleague is a supported workflow and the creator is recorded on the subscription, but listing or deleting them
	 * discloses or changes another user's state and is therefore limited to the subscriber themselves.
	 * <p>
	 * The stored identifier type is the wire form ("adAccount"), which is what {@link Identifier#getTypeString()}
	 * returns. A request without an identifier owns nothing and is refused.
	 */
	private void verifyOwnedByRequestingUser(final SubscriberEntity subscriber) {
		final var owner = subscriber.getIdentifier();
		if (isNull(owner) || !isRequestingUser(owner.getType(), owner.getValue())) {
			throw Problem.valueOf(UNAUTHORIZED, SUBSCRIPTION_NOT_OWNED.formatted(subscriber.getId(), getAdUser()));
		}
	}

	private ErrandEntity resolveErrand(final SubscriptionTarget target, final String namespace, final String municipalityId) {
		if (target.getType() != SubscriptionTargetType.ERRAND) {
			return null;
		}
		// Deliberately no access check: subscribing a colleague to an errand is a supported workflow, and a subscriber
		// only ever receives notifications for errands they themselves may reach, since NotificationDispatchWorker
		// re-evaluates access as the subscriber before every delivery.
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

package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.subscriber.Subscriber;
import se.sundsvall.supportmanagement.integration.db.SubscriberRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriptionRepository;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberSubscriptionCount;
import se.sundsvall.supportmanagement.service.mapper.SubscriberMapper;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SubscriberService {

	private static final String SUBSCRIBER_NOT_FOUND = "Subscriber with id:'%s' not found in namespace:'%s' for municipality with id:'%s'";
	private static final String SUBSCRIBER_CONFLICT = "Subscriber with identifier (type:'%s', value:'%s') and name:'%s' already exists in namespace:'%s' for municipality with id:'%s'";
	private static final String IDENTIFIER_FILTER_INCOMPLETE = "Both identifierType and identifierValue must be provided together";
	private static final String INVALID_PAUSE_WINDOW = "pausedUntil must be after pausedFrom";

	private final SubscriberRepository subscriberRepository;
	private final SubscriptionRepository subscriptionRepository;

	public SubscriberService(final SubscriberRepository subscriberRepository, final SubscriptionRepository subscriptionRepository) {
		this.subscriberRepository = subscriberRepository;
		this.subscriptionRepository = subscriptionRepository;
	}

	@Transactional(readOnly = true)
	public List<Subscriber> findSubscribers(final String municipalityId, final String namespace, final String identifierType, final String identifierValue) {
		final var entities = loadSubscribers(municipalityId, namespace, identifierType, identifierValue);
		if (entities.isEmpty()) {
			return emptyList();
		}
		final var counts = countSubscriptions(entities.stream().map(SubscriberEntity::getId).toList());
		return entities.stream()
			.map(entity -> SubscriberMapper.toSubscriber(entity, counts.getOrDefault(entity.getId(), 0L)))
			.toList();
	}

	private Map<String, Long> countSubscriptions(final List<String> subscriberIds) {
		return subscriptionRepository.countBySubscriberIdIn(subscriberIds).stream()
			.collect(toMap(SubscriberSubscriptionCount::subscriberId, SubscriberSubscriptionCount::count));
	}

	@Transactional(readOnly = true)
	public Subscriber findSubscriber(final String municipalityId, final String namespace, final String subscriberId) {
		final var entity = findEntity(municipalityId, namespace, subscriberId);
		return SubscriberMapper.toSubscriber(entity, subscriptionRepository.countBySubscriberId(subscriberId));
	}

	@Transactional(readOnly = true)
	public SubscriberEntity findEntity(final String municipalityId, final String namespace, final String subscriberId) {
		return subscriberRepository.findByIdAndNamespaceAndMunicipalityId(subscriberId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, SUBSCRIBER_NOT_FOUND.formatted(subscriberId, namespace, municipalityId)));
	}

	@Transactional
	public String createSubscriber(final String municipalityId, final String namespace, final Subscriber subscriber) {
		rejectDuplicate(municipalityId, namespace, subscriber);
		final var entity = SubscriberMapper.toSubscriberEntity(municipalityId, namespace, subscriber);
		entity.setCreatedBy(SubscriberMapper.fromExecutingUser(Identifier.get()));
		validatePauseWindow(entity);
		return persistOrThrowConflict(entity).getId();
	}

	@Transactional
	public Subscriber updateSubscriber(final String municipalityId, final String namespace, final String subscriberId, final Subscriber patch) {
		final var entity = findEntity(municipalityId, namespace, subscriberId);
		SubscriberMapper.applyPatch(entity, patch);
		validatePauseWindow(entity);
		final var saved = persistOrThrowConflict(entity);
		return SubscriberMapper.toSubscriber(saved, subscriptionRepository.countBySubscriberId(subscriberId));
	}

	@Transactional
	public void deleteSubscriber(final String municipalityId, final String namespace, final String subscriberId) {
		final var entity = findEntity(municipalityId, namespace, subscriberId);
		subscriberRepository.delete(entity);
	}

	private List<SubscriberEntity> loadSubscribers(final String municipalityId, final String namespace, final String identifierType, final String identifierValue) {
		if ((identifierType == null) != (identifierValue == null)) {
			throw Problem.valueOf(BAD_REQUEST, IDENTIFIER_FILTER_INCOMPLETE);
		}
		if (identifierType == null) {
			return subscriberRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId);
		}
		return subscriberRepository.findAllByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValue(
			namespace, municipalityId, identifierType, identifierValue);
	}

	private void validatePauseWindow(final SubscriberEntity entity) {
		if (entity.getPausedFrom() != null && entity.getPausedUntil() != null && !entity.getPausedUntil().isAfter(entity.getPausedFrom())) {
			throw Problem.valueOf(BAD_REQUEST, INVALID_PAUSE_WINDOW);
		}
	}

	private void rejectDuplicate(final String municipalityId, final String namespace, final Subscriber subscriber) {
		if (subscriber.getName() == null) {
			return;
		}
		final var identifier = subscriber.getIdentifier();
		final var exists = subscriberRepository.existsByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValueAndName(
			namespace, municipalityId, identifier.getType(), identifier.getValue(), subscriber.getName());
		if (exists) {
			throw conflict(identifier.getType(), identifier.getValue(), subscriber.getName(), namespace, municipalityId);
		}
	}

	// Flush eagerly so the uq_subscriber_municipality_namespace_identifier_name constraint
	// fires inside this method — both on create (TOCTOU race past rejectDuplicate) and on
	// update (where no application-level pre-check runs). Translate to 409 instead of 500.
	private SubscriberEntity persistOrThrowConflict(final SubscriberEntity entity) {
		try {
			return subscriberRepository.saveAndFlush(entity);
		} catch (final DataIntegrityViolationException e) {
			final var identifier = entity.getIdentifier();
			final var type = Optional.ofNullable(identifier).map(IdentifierEmbeddable::getType).orElse(null);
			final var value = Optional.ofNullable(identifier).map(IdentifierEmbeddable::getValue).orElse(null);
			throw conflict(type, value, entity.getName(), entity.getNamespace(), entity.getMunicipalityId());
		}
	}

	private static ThrowableProblem conflict(final String type, final String value, final String name, final String namespace, final String municipalityId) {
		return Problem.valueOf(CONFLICT, SUBSCRIBER_CONFLICT.formatted(type, value, name, namespace, municipalityId));
	}
}

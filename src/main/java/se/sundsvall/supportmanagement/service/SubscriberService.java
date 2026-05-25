package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.subscriber.Subscriber;
import se.sundsvall.supportmanagement.integration.db.SubscriberRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriptionRepository;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.service.mapper.SubscriberMapper;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SubscriberService {

	private static final String SUBSCRIBER_NOT_FOUND = "Subscriber with id:'%s' not found in namespace:'%s' for municipality with id:'%s'";
	private static final String SUBSCRIBER_CONFLICT = "Subscriber with identifier (type:'%s', value:'%s') and name:'%s' already exists in namespace:'%s' for municipality with id:'%s'";
	private static final String IDENTIFIER_FILTER_INCOMPLETE = "Both identifierType and identifierValue must be provided together";

	private final SubscriberRepository subscriberRepository;
	private final SubscriptionRepository subscriptionRepository;

	public SubscriberService(final SubscriberRepository subscriberRepository, final SubscriptionRepository subscriptionRepository) {
		this.subscriberRepository = subscriberRepository;
		this.subscriptionRepository = subscriptionRepository;
	}

	@Transactional(readOnly = true)
	public List<Subscriber> findSubscribers(final String municipalityId, final String namespace, final String identifierType, final String identifierValue) {
		return loadSubscribers(municipalityId, namespace, identifierType, identifierValue).stream()
			.map(entity -> SubscriberMapper.toSubscriber(entity, subscriptionRepository.countBySubscriberId(entity.getId())))
			.toList();
	}

	@Transactional(readOnly = true)
	public Subscriber findSubscriber(final String municipalityId, final String namespace, final String subscriberId) {
		final var entity = loadOrThrow(municipalityId, namespace, subscriberId);
		return SubscriberMapper.toSubscriber(entity, subscriptionRepository.countBySubscriberId(subscriberId));
	}

	@Transactional
	public String createSubscriber(final String municipalityId, final String namespace, final Subscriber subscriber) {
		rejectDuplicate(municipalityId, namespace, subscriber);
		final var entity = SubscriberMapper.toSubscriberEntity(municipalityId, namespace, subscriber);
		entity.setCreatedBy(SubscriberMapper.fromExecutingUser(Identifier.get()));
		return subscriberRepository.save(entity).getId();
	}

	@Transactional
	public Subscriber updateSubscriber(final String municipalityId, final String namespace, final String subscriberId, final Subscriber patch) {
		final var entity = loadOrThrow(municipalityId, namespace, subscriberId);
		SubscriberMapper.applyPatch(entity, patch);
		final var saved = subscriberRepository.save(entity);
		return SubscriberMapper.toSubscriber(saved, subscriptionRepository.countBySubscriberId(subscriberId));
	}

	@Transactional
	public void deleteSubscriber(final String municipalityId, final String namespace, final String subscriberId) {
		final var entity = loadOrThrow(municipalityId, namespace, subscriberId);
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

	private void rejectDuplicate(final String municipalityId, final String namespace, final Subscriber subscriber) {
		if (subscriber.getName() == null) {
			return;
		}
		final var identifier = subscriber.getIdentifier();
		final var exists = subscriberRepository.existsByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValueAndName(
			namespace, municipalityId, identifier.getType(), identifier.getValue(), subscriber.getName());
		if (exists) {
			throw Problem.valueOf(CONFLICT, SUBSCRIBER_CONFLICT.formatted(
				identifier.getType(), identifier.getValue(), subscriber.getName(), namespace, municipalityId));
		}
	}

	private SubscriberEntity loadOrThrow(final String municipalityId, final String namespace, final String subscriberId) {
		return subscriberRepository.findByIdAndNamespaceAndMunicipalityId(subscriberId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, SUBSCRIBER_NOT_FOUND.formatted(subscriberId, namespace, municipalityId)));
	}
}

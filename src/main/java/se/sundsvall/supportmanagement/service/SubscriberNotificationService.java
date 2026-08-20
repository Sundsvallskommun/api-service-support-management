package se.sundsvall.supportmanagement.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.notification.SubscriberNotification;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.SubscriberNotificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.SubscriberNotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor;

import static java.time.OffsetDateTime.now;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_NOTIFICATION_TTL_IN_DAYS;
import static se.sundsvall.supportmanagement.service.mapper.SubscriberNotificationMapper.toEntity;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getAdUser;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.isRequestingUser;

@Service
public class SubscriberNotificationService {

	private static final String NOTIFICATION_NOT_FOUND = "SubscriberNotification with id:'%s' not found in namespace:'%s' for municipality with id:'%s'";
	private static final String NOTIFICATION_NOT_OWNED = "Notifications of '%s' not accessible by user '%s'";

	private final SubscriberNotificationRepository repository;
	private final NamespaceConfigRepository namespaceConfigRepository;

	public SubscriberNotificationService(final SubscriberNotificationRepository repository, final NamespaceConfigRepository namespaceConfigRepository) {
		this.repository = repository;
		this.namespaceConfigRepository = namespaceConfigRepository;
	}

	public Page<SubscriberNotification> getNotifications(final String municipalityId, final String namespace, final String identifierType, final String identifierValue, final Pageable pageable) {
		verifyOwnedByRequestingUser(identifierType, identifierValue);
		return repository.findActiveByMunicipalityIdAndNamespaceAndIdentifierTypeAndIdentifierValue(municipalityId, namespace, identifierType, identifierValue, now(), pageable)
			.map(se.sundsvall.supportmanagement.service.mapper.SubscriberNotificationMapper::toModel);
	}

	@Transactional
	public void deleteNotification(final String municipalityId, final String namespace, final String notificationId) {
		final var entity = findOrThrow(notificationId, municipalityId, namespace);
		verifyOwnedByRequestingUser(entity.getIdentifierType(), entity.getIdentifierValue());
		repository.delete(entity);
	}

	@Transactional
	public void acknowledgeNotification(final String municipalityId, final String namespace, final String notificationId) {
		final var entity = findOrThrow(notificationId, municipalityId, namespace);
		verifyOwnedByRequestingUser(entity.getIdentifierType(), entity.getIdentifierValue());
		entity.setAcknowledged(now());
		repository.save(entity);
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void upsert(final String errandId, final String errandNumber, final SubscriberEntity subscriber) {
		final var namespaceConfig = namespaceConfigRepository.findByNamespaceAndMunicipalityId(subscriber.getNamespace(), subscriber.getMunicipalityId())
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Namespace with name:'%s' and municipalityId '%s' not found!".formatted(subscriber.getNamespace(), subscriber.getMunicipalityId())));
		final var ttlInDays = ConfigPropertyExtractor.<Integer>getValue(namespaceConfig, PROPERTY_NOTIFICATION_TTL_IN_DAYS);

		repository.findByMunicipalityIdAndNamespaceAndErrandIdAndIdentifierTypeAndIdentifierValue(
			subscriber.getMunicipalityId(),
			subscriber.getNamespace(),
			errandId,
			subscriber.getIdentifier().getType(),
			subscriber.getIdentifier().getValue())
			.ifPresentOrElse(
				existing -> {
					existing.setErrandNumber(errandNumber);
					existing.setAcknowledged(null);
					repository.save(existing);
				},
				() -> repository.save(toEntity(errandId, errandNumber, subscriber, ttlInDays)));
	}

	/**
	 * A notification belongs to the identity it was created for, so only that identity may read or change it. This is
	 * ownership rather than access control: the access mapper says nothing about who owns a notification, and being
	 * allowed to reach an errand does not make someone the recipient of another user's notifications about it.
	 * <p>
	 * The stored identifier type is the wire form ("adAccount"), which is what {@link Identifier#getTypeString()}
	 * returns. A request without an identifier owns nothing and is refused.
	 */
	private void verifyOwnedByRequestingUser(final String identifierType, final String identifierValue) {
		if (!isRequestingUser(identifierType, identifierValue)) {
			throw Problem.valueOf(UNAUTHORIZED, NOTIFICATION_NOT_OWNED.formatted(identifierValue, getAdUser()));
		}
	}

	private SubscriberNotificationEntity findOrThrow(final String notificationId, final String municipalityId, final String namespace) {
		return repository.findByIdAndMunicipalityIdAndNamespace(notificationId, municipalityId, namespace)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NOTIFICATION_NOT_FOUND.formatted(notificationId, namespace, municipalityId)));
	}
}

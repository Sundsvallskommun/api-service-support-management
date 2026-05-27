package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.notification.SubscriberNotification;
import se.sundsvall.supportmanagement.integration.db.SubscriberNotificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.SubscriberNotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;

import static java.time.OffsetDateTime.now;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.SubscriberNotificationMapper.toEntity;
import static se.sundsvall.supportmanagement.service.mapper.SubscriberNotificationMapper.toModel;

@Service
public class SubscriberNotificationService {

	private static final String NOTIFICATION_NOT_FOUND = "SubscriberNotification with id:'%s' not found in namespace:'%s' for municipality with id:'%s'";

	private final SubscriberNotificationRepository repository;

	public SubscriberNotificationService(final SubscriberNotificationRepository repository) {
		this.repository = repository;
	}

	public List<SubscriberNotification> getNotifications(final String municipalityId, final String namespace, final String identifierType, final String identifierValue) {
		return repository.findAllByMunicipalityIdAndNamespaceAndIdentifierTypeAndIdentifierValue(municipalityId, namespace, identifierType, identifierValue)
			.stream()
			.map(se.sundsvall.supportmanagement.service.mapper.SubscriberNotificationMapper::toModel)
			.toList();
	}

	@Transactional
	public void deleteNotification(final String municipalityId, final String namespace, final String notificationId) {
		repository.delete(findOrThrow(notificationId, municipalityId, namespace));
	}

	@Transactional
	public void acknowledgeNotification(final String municipalityId, final String namespace, final String notificationId) {
		final var entity = findOrThrow(notificationId, municipalityId, namespace);
		entity.setAcknowledged(now());
		repository.save(entity);
	}

	@Transactional
	public void upsert(final String errandId, final String errandNumber, final SubscriberEntity subscriber) {
		repository.findByMunicipalityIdAndNamespaceAndErrandIdAndIdentifierTypeAndIdentifierValue(
			subscriber.getMunicipalityId(),
			subscriber.getNamespace(),
			errandId,
			subscriber.getIdentifier().getType(),
			subscriber.getIdentifier().getValue())
			.ifPresentOrElse(
				existing -> repository.save(existing),
				() -> repository.save(toEntity(errandId, errandNumber, subscriber)));
	}

	private SubscriberNotificationEntity findOrThrow(final String notificationId, final String municipalityId, final String namespace) {
		return repository.findByIdAndMunicipalityIdAndNamespace(notificationId, municipalityId, namespace)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NOTIFICATION_NOT_FOUND.formatted(notificationId, namespace, municipalityId)));
	}
}

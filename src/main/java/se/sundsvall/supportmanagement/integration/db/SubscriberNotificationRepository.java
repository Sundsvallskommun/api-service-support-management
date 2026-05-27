package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.SubscriberNotificationEntity;

@CircuitBreaker(name = "subscriberNotificationRepository")
public interface SubscriberNotificationRepository extends JpaRepository<SubscriberNotificationEntity, String> {

	List<SubscriberNotificationEntity> findAllByMunicipalityIdAndNamespaceAndIdentifierTypeAndIdentifierValue(
		String municipalityId, String namespace, String identifierType, String identifierValue);

	Optional<SubscriberNotificationEntity> findByIdAndMunicipalityIdAndNamespace(
		String id, String municipalityId, String namespace);

	Optional<SubscriberNotificationEntity> findByMunicipalityIdAndNamespaceAndErrandIdAndIdentifierTypeAndIdentifierValue(
		String municipalityId, String namespace, String errandId, String identifierType, String identifierValue);
}

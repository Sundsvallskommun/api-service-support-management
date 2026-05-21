package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;

@CircuitBreaker(name = "subscriberRepository")
public interface SubscriberRepository extends JpaRepository<SubscriberEntity, String> {

	Optional<SubscriberEntity> findByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	List<SubscriberEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	List<SubscriberEntity> findAllByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValue(
		String namespace, String municipalityId, String identifierType, String identifierValue);

	boolean existsByNamespaceAndMunicipalityIdAndIdentifierTypeAndIdentifierValueAndName(
		String namespace, String municipalityId, String identifierType, String identifierValue, String name);
}

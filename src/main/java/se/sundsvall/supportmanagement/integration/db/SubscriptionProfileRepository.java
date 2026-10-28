package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionProfileEntity;

@CircuitBreaker(name = "subscriptionProfileRepository")
public interface SubscriptionProfileRepository extends JpaRepository<SubscriptionProfileEntity, String> {

	Optional<SubscriptionProfileEntity> findByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	List<SubscriptionProfileEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	boolean existsByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);
}

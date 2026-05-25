package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionTargetType;

@CircuitBreaker(name = "subscriptionRepository")
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, String> {

	List<SubscriptionEntity> findAllBySubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId(
		String subscriberId, String namespace, String municipalityId);

	Optional<SubscriptionEntity> findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId(
		String id, String subscriberId, String namespace, String municipalityId);

	boolean existsBySubscriberIdAndTargetTypeAndErrandId(String subscriberId, SubscriptionTargetType targetType, String errandId);

	boolean existsBySubscriberIdAndTargetTypeAndErrandIsNull(String subscriberId, SubscriptionTargetType targetType);

	long countBySubscriberId(String subscriberId);
}

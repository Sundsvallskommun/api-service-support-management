package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.DbSubscriptionTargetType;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberSubscriptionCount;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;

@CircuitBreaker(name = "subscriptionRepository")
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, String> {

	List<SubscriptionEntity> findAllBySubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId(
		String subscriberId, String namespace, String municipalityId);

	Optional<SubscriptionEntity> findByIdAndSubscriberIdAndSubscriberNamespaceAndSubscriberMunicipalityId(
		String id, String subscriberId, String namespace, String municipalityId);

	boolean existsBySubscriberIdAndTargetTypeAndErrandId(String subscriberId, DbSubscriptionTargetType targetType, String errandId);

	boolean existsBySubscriberIdAndTargetTypeAndErrandIsNull(String subscriberId, DbSubscriptionTargetType targetType);

	long countBySubscriberId(String subscriberId);

	@Query("""
		select new se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberSubscriptionCount(s.subscriber.id, count(s))
		from SubscriptionEntity s
		where s.subscriber.id in :subscriberIds
		group by s.subscriber.id
		""")
	List<SubscriberSubscriptionCount> countBySubscriberIdIn(@Param("subscriberIds") Collection<String> subscriberIds);
}

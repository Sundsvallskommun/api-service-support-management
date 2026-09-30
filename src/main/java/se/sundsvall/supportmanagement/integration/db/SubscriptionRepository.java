package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
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

	/**
	 * Finds every subscription that covers the given errand and is currently able to receive notifications: subscriptions
	 * targeting the whole namespace or that specific errand, that have not expired, and whose subscriber is not within a
	 * pause window.
	 * <p>
	 * Event filters are deliberately not applied here, since they are evaluated per dispatched event rather than per
	 * errand.
	 */
	@Query("""
		select s from SubscriptionEntity s
		join fetch s.subscriber sub
		where sub.municipalityId = :municipalityId
		and sub.namespace = :namespace
		and (s.expiresAt is null or s.expiresAt > :now)
		and (sub.pausedFrom is null
		     or :now < sub.pausedFrom
		     or (sub.pausedUntil is not null and :now >= sub.pausedUntil))
		and (s.targetType = NAMESPACE or (s.targetType = ERRAND and s.errand.id = :errandId))
		order by sub.id
		""")
	List<SubscriptionEntity> findAllActiveForErrand(
		@Param("municipalityId") String municipalityId,
		@Param("namespace") String namespace,
		@Param("errandId") String errandId,
		@Param("now") OffsetDateTime now);
}

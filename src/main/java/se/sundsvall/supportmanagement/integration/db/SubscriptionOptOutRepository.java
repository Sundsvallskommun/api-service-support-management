package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionOptOutEntity;

@CircuitBreaker(name = "subscriptionOptOutRepository")
public interface SubscriptionOptOutRepository extends JpaRepository<SubscriptionOptOutEntity, String> {

	boolean existsBySubscriberIdAndProfileId(String subscriberId, String profileId);

	void deleteBySubscriberIdAndProfileId(String subscriberId, String profileId);

	List<SubscriptionOptOutEntity> findAllByProfileId(String profileId);
}

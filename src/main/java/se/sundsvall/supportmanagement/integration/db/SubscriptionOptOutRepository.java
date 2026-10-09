package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionOptOutEntity;

@CircuitBreaker(name = "subscriptionOptOutRepository")
public interface SubscriptionOptOutRepository extends JpaRepository<SubscriptionOptOutEntity, String> {

	boolean existsByProfileIdAndIdentifierTypeAndIdentifierValue(String profileId, String identifierType, String identifierValue);

	void deleteByProfileIdAndIdentifierTypeAndIdentifierValue(String profileId, String identifierType, String identifierValue);

	List<SubscriptionOptOutEntity> findAllByProfileId(String profileId);
}

package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import se.sundsvall.supportmanagement.integration.db.model.EmailDispatchOutboxEntity;

@CircuitBreaker(name = "emailDispatchOutboxRepository")
public interface EmailDispatchOutboxRepository extends JpaRepository<EmailDispatchOutboxEntity, String> {

	@Query("select distinct e.subscriber.id from EmailDispatchOutboxEntity e")
	List<String> findDistinctSubscriberIds();

	@EntityGraph(attributePaths = {
		"subscriber", "events"
	})
	List<EmailDispatchOutboxEntity> findBySubscriberIdOrderByCreatedAsc(String subscriberId);
}

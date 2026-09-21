package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.EmailDispatchOutboxEntity;

@CircuitBreaker(name = "emailDispatchOutboxRepository")
public interface EmailDispatchOutboxRepository extends JpaRepository<EmailDispatchOutboxEntity, String> {

	List<EmailDispatchOutboxEntity> findByAttemptsLessThanOrderByCreatedAsc(int maxAttempts);
}

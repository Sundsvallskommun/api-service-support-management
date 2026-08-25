package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.supportmanagement.integration.db.model.MessagingOutboxEntity;

@CircuitBreaker(name = "messagingOutboxRepository")
public interface MessagingOutboxRepository extends JpaRepository<MessagingOutboxEntity, String> {

	@Query("""
		SELECT e FROM MessagingOutboxEntity e
		WHERE e.deadLetter = false
		AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)
		ORDER BY e.created
		""")
	List<MessagingOutboxEntity> findProcessable(@Param("now") OffsetDateTime now);
}

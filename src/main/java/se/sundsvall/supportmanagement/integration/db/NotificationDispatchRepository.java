package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;

@CircuitBreaker(name = "notificationDispatchRepository")
public interface NotificationDispatchRepository extends JpaRepository<NotificationDispatchEntity, String> {

	@Query("""
		SELECT d FROM NotificationDispatchEntity d
		WHERE d.deadLetter = false
		AND d.created < :cutoff
		AND (d.nextRetryAt IS NULL OR d.nextRetryAt <= :now)
		ORDER BY d.errandId
		""")
	List<NotificationDispatchEntity> findProcessable(
		@Param("cutoff") OffsetDateTime cutoff,
		@Param("now") OffsetDateTime now);
}

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
		AND (d.nextRetryAt IS NULL OR d.nextRetryAt <= :now)
		AND (
		    (d.requestGroupId IS NULL AND d.created < :cutoff)
		    OR
		    (d.requestGroupId IS NOT NULL AND d.requestGroupId IN (
		        SELECT d2.requestGroupId FROM NotificationDispatchEntity d2
		        WHERE d2.deadLetter = false
		        GROUP BY d2.requestGroupId
		        HAVING MAX(d2.created) < :cutoff
		    ))
		)
		ORDER BY d.errandId, d.requestGroupId
		""")
	List<NotificationDispatchEntity> findProcessable(
		@Param("cutoff") OffsetDateTime cutoff,
		@Param("now") OffsetDateTime now);

	void deleteByDeadLetterTrueAndCreatedBefore(OffsetDateTime cutoff);
}

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

	/**
	 * Finds all entries except those belonging to a request group that may still be growing: a group is held back until
	 * nothing has been added to it since {@code transactionBufferCutoff}. An entry without a request group has no
	 * siblings to wait for and is returned immediately.
	 */
	@Query("""
		SELECT d FROM NotificationDispatchEntity d
		WHERE d.requestGroupId IS NULL
		OR d.requestGroupId IN (
		    SELECT d2.requestGroupId FROM NotificationDispatchEntity d2
		    GROUP BY d2.requestGroupId
		    HAVING MAX(d2.created) < :transactionBufferCutoff
		)
		ORDER BY d.errandId, d.requestGroupId
		""")
	List<NotificationDispatchEntity> findProcessable(@Param("transactionBufferCutoff") OffsetDateTime transactionBufferCutoff);
}

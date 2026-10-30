package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;

@CircuitBreaker(name = "errandActionRepository")
public interface ErrandActionRepository extends JpaRepository<ErrandActionEntity, String> {

	// The actions of a soft deleted errand are left waiting, and are carried out by the first run after it is restored
	List<ErrandActionEntity> findAllByExecuteAfterBeforeAndErrandEntityDeletedIsNull(OffsetDateTime now);
}

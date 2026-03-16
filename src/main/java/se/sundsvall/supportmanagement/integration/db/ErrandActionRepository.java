package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;

@Transactional
@CircuitBreaker(name = "errandActionRepository")
public interface ErrandActionRepository extends JpaRepository<ErrandActionEntity, String> {

	List<ErrandActionEntity> findAllByExecuteAfterBefore(OffsetDateTime now);
}

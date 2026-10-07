package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.TeliaAceWorkItemEntity;

@CircuitBreaker(name = "teliaAceWorkItemRepository")
public interface TeliaAceWorkItemRepository extends JpaRepository<TeliaAceWorkItemEntity, String> {

	List<TeliaAceWorkItemEntity> findAllByOrderByCreatedAsc();
}

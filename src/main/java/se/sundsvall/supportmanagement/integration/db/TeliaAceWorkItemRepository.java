package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.TeliaAceWorkItemEntity;

@CircuitBreaker(name = "teliaAceWorkItemRepository")
public interface TeliaAceWorkItemRepository extends JpaRepository<TeliaAceWorkItemEntity, String> {

	/**
	 * Capped so a run's worst case (each item takes up to the integration's connect + read timeout, 5 s + 30 s today)
	 * fits inside {@code scheduler.telia-ace-work-item.shedlock-lock-at-most-for} (4 minutes): 6 × 35 s = 210 s, with
	 * 30 s to spare. A larger batch risks a second pod picking up the same rows once the lock expires mid-run, and
	 * delivering the same work items twice. Revisit this number together with the timeouts or the lock duration.
	 */
	List<TeliaAceWorkItemEntity> findTop6ByOrderByCreatedAsc();

	void deleteAllByErrandId(String errandId);
}

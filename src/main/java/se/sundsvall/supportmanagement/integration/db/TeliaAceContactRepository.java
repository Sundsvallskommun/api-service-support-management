package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.TeliaAceContactEntity;

@CircuitBreaker(name = "teliaAceContactRepository")
public interface TeliaAceContactRepository extends JpaRepository<TeliaAceContactEntity, String> {

	void deleteAllByErrandId(String errandId);
}

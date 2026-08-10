package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;

@CircuitBreaker(name = "jsonParameterRepository")
public interface JsonParameterRepository extends JpaRepository<JsonParameterEntity, String> {
}

package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.HandoverIdempotencyEntity;

@CircuitBreaker(name = "handoverIdempotencyRepository")
public interface HandoverIdempotencyRepository extends JpaRepository<HandoverIdempotencyEntity, String> {

	Optional<HandoverIdempotencyEntity> findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(String sourceErrandId, String targetNamespace, String targetMunicipalityId);

}

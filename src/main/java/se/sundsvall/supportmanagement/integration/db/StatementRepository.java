package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.StatementEntity;

/**
 * Reads the statements of an errand through its errand.
 * <p>
 * Every lookup names the namespace, the municipality and the errand, so asking for a statement of another errand finds
 * nothing.
 */
@CircuitBreaker(name = "statementRepository")
public interface StatementRepository extends JpaRepository<StatementEntity, String> {

	Optional<StatementEntity> findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(String namespace, String municipalityId, String errandId, String id);

	List<StatementEntity> findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(String namespace, String municipalityId, String errandId);
}

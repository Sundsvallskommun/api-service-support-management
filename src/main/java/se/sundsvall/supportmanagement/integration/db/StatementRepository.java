package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.StatementEntity;

/**
 * Reads the statements of an errand through its errand.
 * <p>
 * Every lookup names the namespace, the municipality and the errand, so that belonging to the errand is a consequence
 * of the query rather than a check somebody has to remember. Asking for an artefact of another errand finds nothing,
 * and the caller turns that into a 404.
 */
@CircuitBreaker(name = "statementRepository")
public interface StatementRepository extends JpaRepository<StatementEntity, String> {

	Optional<StatementEntity> findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(String namespace, String municipalityId, String errandId, String id);

	List<StatementEntity> findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(String namespace, String municipalityId, String errandId);
}

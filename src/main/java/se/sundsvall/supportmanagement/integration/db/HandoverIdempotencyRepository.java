package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.HandoverIdempotencyEntity;

@CircuitBreaker(name = "handoverIdempotencyRepository")
public interface HandoverIdempotencyRepository extends JpaRepository<HandoverIdempotencyEntity, String> {

	Optional<HandoverIdempotencyEntity> findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(String sourceErrandId, String targetNamespace, String targetMunicipalityId);

	/**
	 * Removes every handover record naming the errand, at either end of the handover. A purged errand may be the one a
	 * handover started from as well as the one it created.
	 *
	 * @param  sourceErrandId the id of the errand as the source of a handover.
	 * @param  newErrandId    the id of the errand as the one a handover created.
	 * @return                the number of removed records.
	 */
	long deleteAllBySourceErrandIdOrNewErrandId(String sourceErrandId, String newErrandId);
}

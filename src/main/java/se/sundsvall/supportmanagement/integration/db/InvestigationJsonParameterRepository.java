package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationJsonParameterEntity;

/**
 * Saves the JSON parameter links of an artefact.
 * <p>
 * It exists for the saving alone, and that is a consequence of the mapping rather than an oversight: the collection on
 * the artefact carries MERGE and REMOVE but not PERSIST, so a new link is not written by cascading from the artefact
 * and has to be saved here. What leaving PERSIST out buys is that a link the other side has just removed cannot be
 * written back by the next flush.
 * <p>
 * Reading and removing go through the collection on the artefact, which is loaded anyway to authorize the call.
 */
@CircuitBreaker(name = "investigationJsonParameterRepository")
public interface InvestigationJsonParameterRepository extends JpaRepository<InvestigationJsonParameterEntity, String> {
}

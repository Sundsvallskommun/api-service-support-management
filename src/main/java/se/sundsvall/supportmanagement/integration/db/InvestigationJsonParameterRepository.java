package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationJsonParameterEntity;

/**
 * The JSON parameter links of investigations. See {@link StatementJsonParameterRepository} for how they are written and
 * removed.
 */
@CircuitBreaker(name = "investigationJsonParameterRepository")
public interface InvestigationJsonParameterRepository extends JpaRepository<InvestigationJsonParameterEntity, String> {

	List<InvestigationJsonParameterEntity> findByJsonParameterEntityErrandEntityId(String errandId);
}

package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.DecisionJsonParameterEntity;

/**
 * The JSON parameter links of decisions. See {@link StatementJsonParameterRepository} for how they are written and
 * removed.
 */
@CircuitBreaker(name = "decisionJsonParameterRepository")
public interface DecisionJsonParameterRepository extends JpaRepository<DecisionJsonParameterEntity, String> {

	List<DecisionJsonParameterEntity> findByJsonParameterEntityErrandEntityId(String errandId);

	boolean existsByJsonParameterEntityId(String jsonParameterId);
}

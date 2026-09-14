package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.MeasureJsonParameterEntity;

/**
 * The JSON parameter links of measures. See {@link StatementJsonParameterRepository} for how they are written and
 * removed.
 */
@CircuitBreaker(name = "measureJsonParameterRepository")
public interface MeasureJsonParameterRepository extends JpaRepository<MeasureJsonParameterEntity, String> {

	List<MeasureJsonParameterEntity> findByJsonParameterEntityErrandEntityId(String errandId);
}

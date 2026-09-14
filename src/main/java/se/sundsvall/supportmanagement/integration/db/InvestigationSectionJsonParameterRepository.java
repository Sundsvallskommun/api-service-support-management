package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationSectionJsonParameterEntity;

/**
 * The JSON parameter links of investigation sections. See {@link StatementJsonParameterRepository} for how they are
 * written and removed.
 */
@CircuitBreaker(name = "investigationSectionJsonParameterRepository")
public interface InvestigationSectionJsonParameterRepository extends JpaRepository<InvestigationSectionJsonParameterEntity, String> {

	List<InvestigationSectionJsonParameterEntity> findByJsonParameterEntityErrandEntityId(String errandId);
}

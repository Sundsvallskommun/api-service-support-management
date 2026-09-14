package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.StatementJsonParameterEntity;

/**
 * The JSON parameter links of statements.
 * <p>
 * A new link is saved here rather than by cascading from the statement, whose collection carries MERGE alone, and the
 * database removes it together with its parameter or its statement. Reading the links of one statement goes through the
 * collection on it, which is loaded anyway to authorize the call.
 */
@CircuitBreaker(name = "statementJsonParameterRepository")
public interface StatementJsonParameterRepository extends JpaRepository<StatementJsonParameterEntity, String> {

	/**
	 * The links naming a parameter of the errand, which is what a patch of the errand asks to learn which of them it may
	 * not change.
	 */
	List<StatementJsonParameterEntity> findByJsonParameterEntityErrandEntityId(String errandId);

	/**
	 * Whether a link names the parameter, which is what the JSON parameter endpoints of the errand ask before writing or
	 * removing it.
	 */
	boolean existsByJsonParameterEntityId(String jsonParameterId);
}

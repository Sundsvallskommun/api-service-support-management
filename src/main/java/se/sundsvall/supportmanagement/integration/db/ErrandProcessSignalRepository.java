package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessSignalEntity;

@Transactional
@CircuitBreaker(name = "errandProcessSignalRepository")
public interface ErrandProcessSignalRepository extends JpaRepository<ErrandProcessSignalEntity, String> {

	/**
	 * What one instance waits for right now, in the order the process reported it.
	 *
	 * @param  errandProcessId the instance to look at.
	 * @return                 the signals the instance waits for, and empty when it waits for no person.
	 */
	List<ErrandProcessSignalEntity> findByErrandProcessIdOrderBySortOrderAsc(String errandProcessId);

	/**
	 * What several instances wait for, read in one query for all of them.
	 *
	 * @param  errandProcessIds the instances to look at.
	 * @return                  the signals of those instances, each instance's in the order the process reported them.
	 */
	List<ErrandProcessSignalEntity> findByErrandProcessIdInOrderBySortOrderAsc(Collection<String> errandProcessIds);
}

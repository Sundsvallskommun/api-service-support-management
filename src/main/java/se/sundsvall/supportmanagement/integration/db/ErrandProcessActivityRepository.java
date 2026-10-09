package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;

@Transactional
@CircuitBreaker(name = "errandProcessActivityRepository")
public interface ErrandProcessActivityRepository extends JpaRepository<ErrandProcessActivityEntity, String> {

	/**
	 * The log of an errand, including the entries written without an instance, such as those explaining why no process
	 * started.
	 *
	 * @param  errandId the errand to read.
	 * @param  pageable the page to read.
	 * @return          the entries written for the errand.
	 */
	Page<ErrandProcessActivityEntity> findByErrandId(String errandId, Pageable pageable);

	/**
	 * The log of one instance of an errand.
	 *
	 * @param  errandId        the errand to read.
	 * @param  errandProcessId the instance to narrow the reading to.
	 * @param  pageable        the page to read.
	 * @return                 the entries written for that instance.
	 */
	Page<ErrandProcessActivityEntity> findByErrandIdAndErrandProcessId(String errandId, String errandProcessId, Pageable pageable);

	/**
	 * The entries an external task has already written for an instance.
	 * <p>
	 * Read before a report is stored, so that a replayed report adds nothing. Entries without an external task are never
	 * matched.
	 *
	 * @param  errandProcessId the instance the report belongs to.
	 * @param  externalTaskId  the external task the report was made from.
	 * @return                 the entries already written for that task.
	 */
	List<ErrandProcessActivityEntity> findByErrandProcessIdAndExternalTaskId(String errandProcessId, String externalTaskId);

	/**
	 * Whether an errand already carries an entry for a fault inside a window.
	 * <p>
	 * Used to write the entries that report a jammed errand once per errand, fault and window.
	 *
	 * @param  errandId     the errand to look at.
	 * @param  errorCode    the code of the fault to look for.
	 * @param  createdAfter the start of the window.
	 * @return              whether such an entry has already been written inside the window.
	 */
	boolean existsByErrandIdAndErrorCodeAndCreatedAfter(String errandId, String errorCode, OffsetDateTime createdAfter);

	/**
	 * Whether an instance already carries an entry of a kind.
	 * <p>
	 * Asked before the warning about two work steps running at once is written, so that it is written once per instance.
	 *
	 * @param  errandProcessId the instance to look at.
	 * @param  activityType    the kind of entry to look for.
	 * @return                 whether such an entry has already been written for the instance.
	 */
	boolean existsByErrandProcessIdAndActivityType(String errandProcessId, String activityType);

	/**
	 * Entries old enough to be swept, oldest first. Retention runs on the SM clock, not on the clock of the process.
	 *
	 * @param  createdBefore the moment an entry has to predate to be swept.
	 * @param  pageable      the batch limit for the run.
	 * @return               the entries that have aged out.
	 */
	List<ErrandProcessActivityEntity> findByCreatedBeforeOrderByCreatedAsc(OffsetDateTime createdBefore, Pageable pageable);
}

package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity;

@Transactional
@CircuitBreaker(name = "errandProcessActivityRepository")
public interface ErrandProcessActivityRepository extends JpaRepository<ErrandProcessActivityEntity, String> {

	/**
	 * The log of an errand.
	 * <p>
	 * Read per errand rather than per instance, since the entries that explain why no process started have no instance to
	 * be found by.
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
	 * Read before a report is stored, so that a replayed report adds nothing: {@code uq_epa_idempotency} would refuse the
	 * duplicate, but a constraint violation poisons the transaction the rest of the report is being written in, and
	 * asking first is what keeps a retry a plain success rather than an error to recover from. Entries without an external
	 * task are never matched here, which is the same answer the constraint gives, since null is distinct in a unique
	 * index.
	 *
	 * @param  errandProcessId the instance the report belongs to.
	 * @param  externalTaskId  the external task the report was made from.
	 * @return                 the entries already written for that task.
	 */
	List<ErrandProcessActivityEntity> findByErrandProcessIdAndExternalTaskId(String errandProcessId, String externalTaskId);

	/**
	 * Whether an errand already carries an entry of a kind inside a window.
	 * <p>
	 * What it is for is to write the entries that report a jammed errand once per errand and window instead of once per
	 * discarded event. {@code uq_epa_idempotency} does not help there: both the instance and the external task are null
	 * for those entries, and null is distinct in a unique index, so the error would drown the log it is reported in.
	 *
	 * @param  errandId     the errand to look at.
	 * @param  activityType the kind of entry to look for.
	 * @param  severity     the severity to look for.
	 * @param  createdAfter the start of the window.
	 * @return              whether such an entry has already been written inside the window.
	 */
	boolean existsByErrandIdAndActivityTypeAndSeverityAndCreatedAfter(String errandId, String activityType, ActivitySeverity severity, OffsetDateTime createdAfter);

	/**
	 * Whether an instance already carries an entry of a kind.
	 * <p>
	 * Asked before the warning about two work steps running at once is written, so that it is written once per
	 * instance while the counter takes every occurrence. Branches that pass each other do so for as long as the model
	 * has the gateway, and a log the handler reads would drown in a fault it has already been told about.
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

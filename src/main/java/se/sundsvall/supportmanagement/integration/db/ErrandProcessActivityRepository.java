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
	 * Entries old enough to be swept, oldest first. Retention runs on the SM clock, not on the clock of the process.
	 *
	 * @param  createdBefore the moment an entry has to predate to be swept.
	 * @param  pageable      the batch limit for the run.
	 * @return               the entries that have aged out.
	 */
	List<ErrandProcessActivityEntity> findByCreatedBeforeOrderByCreatedAsc(OffsetDateTime createdBefore, Pageable pageable);
}

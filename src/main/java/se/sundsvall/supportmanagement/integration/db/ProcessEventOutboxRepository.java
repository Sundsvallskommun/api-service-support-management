package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

@Transactional
@CircuitBreaker(name = "processEventOutboxRepository")
public interface ProcessEventOutboxRepository extends JpaRepository<ProcessEventOutboxEntity, String> {

	/**
	 * The rows waiting for one process engine, oldest first.
	 * <p>
	 * The page bounds how many rows one run takes. Only rows written before {@code createdBefore} are read, which leaves
	 * rows written by a transaction still being committed alone.
	 *
	 * @param  processService the consumer the rows were addressed to when they were written.
	 * @param  createdBefore  the moment a row has to predate to be picked up.
	 * @param  pageable       the batch limit and ordering for the run.
	 * @return                the undelivered rows for the consumer.
	 */
	List<ProcessEventOutboxEntity> findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore(String processService, OffsetDateTime createdBefore, Pageable pageable);

	/**
	 * The same rows, leaving out those of some errands: the errands a run has already failed to deliver.
	 *
	 * @param  processService the consumer the rows were addressed to when they were written.
	 * @param  createdBefore  the moment a row has to predate to be picked up.
	 * @param  errandIds      the errands whose rows to leave out. Must not be empty.
	 * @param  pageable       the batch limit and ordering for the run.
	 * @return                the undelivered rows for the consumer of every other errand.
	 */
	List<ProcessEventOutboxEntity> findByProcessServiceAndDeliveredAtIsNullAndCreatedBeforeAndErrandIdNotIn(String processService, OffsetDateTime createdBefore, Collection<String> errandIds,
		Pageable pageable);

	/**
	 * The rows of one errand waiting for a process engine, oldest first, for a direct run to deliver.
	 * <p>
	 * A row is read however recently it was written, but not once it is older than {@code createdAfter}: a row that has
	 * aged out is left to be dropped.
	 *
	 * @param  processService the consumer the rows were addressed to when they were written.
	 * @param  errandId       the errand whose rows to read.
	 * @param  createdAfter   the moment a row has to be newer than, which is where the age limit lies.
	 * @param  pageable       the batch limit for the run.
	 * @return                the undelivered rows of the errand.
	 */
	List<ProcessEventOutboxEntity> findByProcessServiceAndErrandIdAndDeliveredAtIsNullAndCreatedAfterOrderByCreatedAscIdAsc(String processService, String errandId, OffsetDateTime createdAfter, Pageable pageable);

	/**
	 * Rows read again under a write lock, keeping those still undelivered.
	 * <p>
	 * When two runs reach for the same row, the second waits for the first to commit and then finds the row delivered.
	 * The rows are read by primary key, so the lock covers them and nothing around them: a publication writing a new row
	 * is not held up by a delivery in progress. The rows come in no particular order; the caller puts them in order.
	 *
	 * @param  ids the rows to lock.
	 * @return     those of the rows that are still undelivered, locked for the rest of the transaction.
	 */
	@Lock(PESSIMISTIC_WRITE)
	List<ProcessEventOutboxEntity> findByIdInAndDeliveredAtIsNull(Collection<String> ids);

	/**
	 * Undelivered rows written before a moment, oldest first: the rows that have aged out. Read without a lock; the caller
	 * locks the rows by id before it drops them.
	 *
	 * @param  createdBefore the moment a row has to predate.
	 * @param  pageable      the most rows to take.
	 * @return               the rows that have aged out.
	 */
	List<ProcessEventOutboxEntity> findByDeliveredAtIsNullAndCreatedBeforeOrderByCreatedAscIdAsc(OffsetDateTime createdBefore, Pageable pageable);

	/**
	 * Rows delivered before a moment, for the cleanup to remove. Undelivered rows are never returned.
	 *
	 * @param  deliveredBefore the moment a row has to have been delivered before.
	 * @param  pageable        the most rows to take.
	 * @return                 the rows delivered before the moment.
	 */
	List<ProcessEventOutboxEntity> findByDeliveredAtBefore(OffsetDateTime deliveredBefore, Pageable pageable);

	/**
	 * How much has reached the process engine for one errand lately, which is what the emergency brake measures. Only
	 * delivered rows are counted.
	 *
	 * @param  errandId     the errand to measure.
	 * @param  createdAfter the start of the window.
	 * @return              the number of delivered rows written for the errand inside the window.
	 */
	long countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(String errandId, OffsetDateTime createdAfter);

	/**
	 * The starts of one errand still on their way to the process engine: its undelivered rows carrying the permission to
	 * start a process, whether a start command or an ordinary errand event gave it. Covered by {@code idx_peo_guard}.
	 *
	 * @param  errandId the errand whose starts to read.
	 * @return          the undelivered rows of the errand that carry the permission to start a process.
	 */
	List<ProcessEventOutboxEntity> findByErrandIdAndStartAllowedIsTrueAndDeliveredAtIsNull(String errandId);

	/**
	 * Whether a start of one errand is still on its way to the process engine. Covered by {@code idx_peo_guard}.
	 *
	 * @param  errandId the errand to ask about.
	 * @return          whether an undelivered row of the errand carries the permission to start a process.
	 */
	boolean existsByErrandIdAndStartAllowedIsTrueAndDeliveredAtIsNull(String errandId);

	/**
	 * The oldest undelivered row, whose age the health check of the relay measures.
	 *
	 * @return the oldest undelivered row, or empty when every row has been delivered.
	 */
	Optional<ProcessEventOutboxEntity> findFirstByDeliveredAtIsNullOrderByCreatedAsc();

	/**
	 * Whether any undelivered row is addressed to another process consumer than the sent in one, which no run delivers.
	 *
	 * @param  processService the process consumer rows are delivered to.
	 * @return                whether an undelivered row is addressed anywhere else.
	 */
	boolean existsByDeliveredAtIsNullAndProcessServiceNot(String processService);
}

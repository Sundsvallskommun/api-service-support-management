package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
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
	 * The page is what keeps a single run from being unbounded, and the age limit leaves rows written by a transaction
	 * still being committed alone.
	 *
	 * @param  processService the consumer the rows were addressed to when they were written.
	 * @param  createdBefore  the moment a row has to predate to be picked up.
	 * @param  pageable       the batch limit and ordering for the run.
	 * @return                the undelivered rows for the consumer.
	 */
	List<ProcessEventOutboxEntity> findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore(String processService, OffsetDateTime createdBefore, Pageable pageable);

	/**
	 * The rows of one errand waiting for a process engine, oldest first, which is what a direct run delivers.
	 * <p>
	 * Not held to the age a row must reach before the scheduled run takes it, since a direct run starts only once the
	 * transaction that wrote the row is committed. Held to the age limit instead: a row that has aged out is left to be
	 * dropped rather than delivered after all.
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
	 * The lock is what lets two runs reach for the same row: the second waits for the first to commit, and then finds the
	 * row delivered. Read by id and in no particular order, which leaves the primary key the only sensible way to the
	 * rows, so the lock covers them and nothing around them - a publication writing a new row is not held up by a
	 * delivery in progress. The caller puts the rows in order.
	 *
	 * @param  ids the rows to lock.
	 * @return     those of the rows that are still undelivered, locked for the rest of the transaction.
	 */
	@Lock(PESSIMISTIC_WRITE)
	List<ProcessEventOutboxEntity> findByIdInAndDeliveredAtIsNull(Collection<String> ids);

	/**
	 * Undelivered rows written before a moment, oldest first, which is how the rows that have aged out are found. Read
	 * without a lock: the rows are locked by id before they are dropped.
	 *
	 * @param  createdBefore the moment a row has to predate.
	 * @param  pageable      the most rows to take.
	 * @return               the rows that have aged out.
	 */
	List<ProcessEventOutboxEntity> findByDeliveredAtIsNullAndCreatedBeforeOrderByCreatedAscIdAsc(OffsetDateTime createdBefore, Pageable pageable);

	/**
	 * Rows delivered before a moment, which is what the cleanup removes. Undelivered rows never answer, since their
	 * delivery time is null.
	 *
	 * @param  deliveredBefore the moment a row has to have been delivered before.
	 * @param  pageable        the most rows to take.
	 * @return                 the rows delivered before the moment.
	 */
	List<ProcessEventOutboxEntity> findByDeliveredAtBefore(OffsetDateTime deliveredBefore, Pageable pageable);

	/**
	 * How much has actually reached the process engine for one errand lately, which is what the emergency brake measures.
	 * <p>
	 * Only delivered rows count. Counting the undelivered ones would let a delivery outage trip the brake by itself: the
	 * rows pile up because nothing gets through, the brake reads the pile as a loop, and an outage that only cost time
	 * turns into permanent event loss.
	 *
	 * @param  errandId     the errand to measure.
	 * @param  createdAfter the start of the window.
	 * @return              the number of delivered rows written for the errand inside the window.
	 */
	long countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter(String errandId, OffsetDateTime createdAfter);

	/**
	 * Undelivered rows, oldest first.
	 * <p>
	 * Health is measured in the age of the oldest one, not in how many there are: every publication leaves a row behind
	 * until the next run takes it, so a condition on existence would report unhealthy during normal operation and teach
	 * everyone to stop looking.
	 *
	 * @param  pageable how many of the oldest to look at.
	 * @return          the undelivered rows, oldest first.
	 */
	List<ProcessEventOutboxEntity> findByDeliveredAtIsNullOrderByCreatedAsc(Pageable pageable);

	/**
	 * Whether any undelivered row is addressed to another process consumer than the sent in one, which no run delivers.
	 *
	 * @param  processService the process consumer rows are delivered to.
	 * @return                whether an undelivered row is addressed anywhere else.
	 */
	boolean existsByDeliveredAtIsNullAndProcessServiceNot(String processService);
}

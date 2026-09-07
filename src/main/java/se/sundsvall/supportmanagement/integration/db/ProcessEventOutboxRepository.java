package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;

@Transactional
@CircuitBreaker(name = "processEventOutboxRepository")
public interface ProcessEventOutboxRepository extends JpaRepository<ProcessEventOutboxEntity, String> {

	/**
	 * The rows waiting for one process engine, oldest first.
	 * <p>
	 * Asking per consumer is what keeps one engine being down from starving the others, and the page is what keeps a
	 * single run from being unbounded. The age limit leaves rows written by a transaction still being committed alone.
	 *
	 * @param  processService the consumer the rows were addressed to when they were written.
	 * @param  createdBefore  the moment a row has to predate to be picked up.
	 * @param  pageable       the batch limit and ordering for the run.
	 * @return                the undelivered rows for the consumer.
	 */
	List<ProcessEventOutboxEntity> findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore(String processService, OffsetDateTime createdBefore, Pageable pageable);

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
}

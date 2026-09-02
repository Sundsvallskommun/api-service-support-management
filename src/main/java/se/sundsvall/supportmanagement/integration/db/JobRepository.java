package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.JobEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobType;

@Transactional
@CircuitBreaker(name = "jobRepository")
public interface JobRepository extends JpaRepository<JobEntity, String> {

	Optional<JobEntity> findByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	/**
	 * A job of one kind, for a caller that reaches the job table through a resource of its own and has no business
	 * touching the work of another kind that happens to share the table.
	 */
	Optional<JobEntity> findByIdAndNamespaceAndMunicipalityIdAndType(String id, String namespace, String municipalityId, JobType type);

	boolean existsByNamespaceAndMunicipalityIdAndStatusIn(String namespace, String municipalityId, Collection<JobStatus> statuses);

	boolean existsByNamespaceAndMunicipalityIdAndTypeAndStatusIn(String namespace, String municipalityId, JobType type, Collection<JobStatus> statuses);

	/**
	 * Jobs in one of the sent in states that have not been written to since the sent in point in time.
	 *
	 * @param  statuses the states to look among.
	 * @param  before   the point in time a job must have gone quiet before to be returned.
	 * @return          the jobs last written to before the sent in point in time.
	 */
	List<JobEntity> findByStatusInAndModifiedBefore(Collection<JobStatus> statuses, OffsetDateTime before);

	/**
	 * Jobs in one of the sent in states that have never been written to since they were created, and were created before
	 * the sent in point in time.
	 * <p>
	 * A job gets a modified of its own the first time the work reports on it, so one that never got that far has none to
	 * be found by and is reached through the moment it was created instead.
	 *
	 * @param  statuses the states to look among.
	 * @param  before   the point in time a job must have been created before to be returned.
	 * @return          the jobs never reported on that were created before the sent in point in time.
	 */
	List<JobEntity> findByStatusInAndModifiedIsNullAndCreatedBefore(Collection<JobStatus> statuses, OffsetDateTime before);

}

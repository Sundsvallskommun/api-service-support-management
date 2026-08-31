package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

@CircuitBreaker(name = "errandsRepository")
public interface ErrandsRepository extends JpaRepository<ErrandEntity, String>, JpaSpecificationExecutor<ErrandEntity> {

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	// Locks row in transaction. Other threads will wait until lock is released.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	boolean existsWithLockingByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	// Locks row in transaction. Other threads will wait until lock is released.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ErrandEntity> findWithLockingById(String id);

	Optional<ErrandEntity> findByErrandNumberAndNamespaceAndMunicipalityId(String errandNumber, String namespace, String municipalityId);

	Optional<ErrandEntity> findByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	List<ErrandEntity> findAllBySuspendedToBefore(OffsetDateTime now);

	boolean existsByLabelsMetadataLabelIdIn(Collection<String> labelIds);

	boolean existsByPhasesPhaseEntityId(String phaseId);

	/**
	 * Ids of the errands in a namespace that have not been touched since the provided cutoff, in ascending id order and
	 * starting after the provided id.
	 * <p>
	 * Keyed on the id rather than paged by offset, because a purge deletes as it walks. An offset would step over the
	 * errands that move up behind a deleted one, and reading the first page over and over would never get past an errand
	 * that fails to delete - nor past any errand at all during a dry run, which deletes nothing.
	 * <p>
	 * An errand carrying no timestamp at all is left alone: one that cannot be dated cannot be shown to be old enough to
	 * remove, and the coalesce leaves it out by answering null.
	 *
	 * @param  namespace      the namespace to purge within.
	 * @param  municipalityId the id of the municipality to purge within.
	 * @param  cutoff         errands last touched before this point in time are returned.
	 * @param  afterId        the id the previous batch ended on, or an empty string to start from the beginning.
	 * @param  pageable       the size of the batch to read.
	 * @return                ids of the matching errands.
	 */
	@Query("""
		select e.id from ErrandEntity e
		where e.namespace = :namespace
		and e.municipalityId = :municipalityId
		and e.id > :afterId
		and coalesce(e.touched, e.modified, e.created) < :cutoff
		order by e.id asc
		""")
	List<String> findIdsToPurge(
		@Param("namespace") String namespace,
		@Param("municipalityId") String municipalityId,
		@Param("cutoff") OffsetDateTime cutoff,
		@Param("afterId") String afterId,
		Pageable pageable);

}

package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.IdProjection;

import static org.springframework.data.domain.Sort.Direction.ASC;

@CircuitBreaker(name = "errandsRepository")
public interface ErrandsRepository extends JpaRepository<ErrandEntity, String>, JpaSpecificationExecutor<ErrandEntity> {

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	// Locks row in transaction. Other threads will wait until lock is released.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	boolean existsWithLockingByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	// Locks row in transaction. Other threads will wait until lock is released.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ErrandEntity> findWithLockingById(String id);

	// As the two above, leaving out a soft deleted errand, which the API holds to be gone until it is restored
	boolean existsByIdAndNamespaceAndMunicipalityIdAndDeletedIsNull(String id, String namespace, String municipalityId);

	// Locks row in transaction. Other threads will wait until lock is released.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	boolean existsWithLockingByIdAndNamespaceAndMunicipalityIdAndDeletedIsNull(String id, String namespace, String municipalityId);

	Optional<ErrandEntity> findByErrandNumberAndNamespaceAndMunicipalityId(String errandNumber, String namespace, String municipalityId);

	Optional<ErrandEntity> findByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	Optional<ErrandEntity> findByIdAndNamespaceAndMunicipalityIdAndDeletedIsNull(String id, String namespace, String municipalityId);

	boolean existsByIdAndDeletedIsNull(String id);

	// A soft deleted errand is left suspended, and is woken up by the first run after it is restored
	@Query("""
		select e.id from ErrandEntity e
		where e.status = :status
		  and e.suspendedTo < :now
		  and e.deleted is null
		  and (e.suspensionEndNotifiedFor is null or e.suspensionEndNotifiedFor <> e.suspendedTo)
		""")
	List<String> findIdsWithUnannouncedSuspensionEndAndDeletedIsNull(@Param("status") String status, @Param("now") OffsetDateTime now);

	// A query rather than a save, since this is bookkeeping and no change to the errand: modified, touched, the version
	// and the search index are all left as they are.
	@Modifying
	@Query("update ErrandEntity e set e.suspensionEndNotifiedFor = :suspendedTo where e.id = :id")
	void markSuspensionEndNotified(@Param("id") String id, @Param("suspendedTo") OffsetDateTime suspendedTo);

	boolean existsByLabelsMetadataLabelIdIn(Collection<String> labelIds);

	// accessLabels, not labels: labels carries every ancestor of whatever an errand is actually tagged with, so a
	// label's own id shows up there for any errand tagged with one of its descendants too - not what "is this label
	// itself still directly referenced" (the question a label-tree restructure's DELETE step needs answered,
	// independent of what an earlier step in the same request did to its former descendants) should be asking.
	// accessLabels holds only an errand's own leaf-level tags, which is exactly that question.
	boolean existsByAccessLabelsMetadataLabelIdIn(Collection<String> labelIds);

	long countByLabelsMetadataLabelId(String metadataLabelId);

	// Distinct: an errand tagged with more than one of the given ids (e.g. the moved label itself and one of its
	// descendants, both stored per the ancestor-chain-expansion invariant) must still be counted once.
	long countDistinctByLabelsMetadataLabelIdIn(Collection<String> metadataLabelIds);

	List<ErrandEntity> findAllByLabelsMetadataLabelId(String metadataLabelId);

	// accessLabels is lazy by default; the label-move worker reads it on an already-detached entity (the page fetch and
	// the persist that follows it are each their own transaction), so it must come back populated with the page itself.
	//
	// Keyset paging (id > lastSeenId), not offset: an errand created, purged, or (un)labelled while the worker walks
	// the set shifts what an offset-based page would return, and an errand landing on the boundary would be skipped.
	// A UUID id sorts after "" so "" is the lower bound the first page is read with.
	@EntityGraph(attributePaths = "accessLabels")
	List<ErrandEntity> findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(String metadataLabelId, String id, Pageable pageable);

	// Sibling of the single-id query above, for the label-merge worker walking several source labels at once - same
	// keyset paging, same reason for it.
	@EntityGraph(attributePaths = "accessLabels")
	List<ErrandEntity> findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(Collection<String> metadataLabelIds, String id, Pageable pageable);

	/**
	 * Locks every errand in {@code ids} with {@code SELECT ... FOR UPDATE}, ordered by id - used by the label-
	 * restructure runner, which restows a whole run inside one transaction and needs every affected errand locked
	 * against a concurrent write for the duration rather than retried against one. Ordered acquisition, combined
	 * with the runner chunking a globally-sorted id list, keeps lock acquisition ascending across the whole run, so
	 * it cannot deadlock against another ordered multi-row operation (a user PATCHing several errands,
	 * {@code ErrandPurgeRunner}'s own ascending-id walk).
	 * <p>
	 * No lock-timeout hint - left at the server/session default ({@code innodb_lock_wait_timeout}, 50s), deliberately:
	 * a brief conflict is worth waiting through rather than bouncing the run, and the default can be raised later,
	 * without a code change, if that ever proves too tight.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from ErrandEntity e where e.id in :ids order by e.id")
	List<ErrandEntity> findAllByIdForUpdate(@Param("ids") Collection<String> ids);

	boolean existsByPhasesPhaseEntityId(String phaseId);

}

package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

	// accessLabels, not labels: labels carries every ancestor of whatever an errand is actually tagged with, so a
	// label's own id shows up there for any errand tagged with one of its descendants too - not what "is this label
	// itself still directly referenced" (the question a label-tree restructure's DELETE step needs answered,
	// independent of what an earlier step in the same request did to its former descendants) should be asking.
	// accessLabels holds only an errand's own leaf-level tags, which is exactly that question.
	boolean existsByAccessLabelsMetadataLabelIdIn(Collection<String> labelIds);

	long countByLabelsMetadataLabelId(String metadataLabelId);

	List<ErrandEntity> findAllByLabelsMetadataLabelId(String metadataLabelId);

	/**
	 * The ids of every errand tagged with any of the given label ids - distinct, since an errand tagged with more than
	 * one of them (e.g. the moved/merged label itself and one of its descendants, both stored per the
	 * ancestor-chain-expansion invariant) must still count once.
	 * <p>
	 * The single source of truth for "which errands does this operation affect": a label-move or -merge dry run reads
	 * its {@code affectedErrandCount} from {@code size()} here, and the job this starts resolves the same list once at
	 * the outset and carries it through {@code LabelMoveRun}/{@code LabelMergeRun} so the job's own {@code total} and
	 * the walk that restows them read from the exact same set - see the review discussion on
	 * {@code LabelMoveRunner#restowErrands} for what went wrong when the walk instead re-derived its own, narrower set
	 * from a single label id.
	 */
	@Query("select distinct e.id from ErrandEntity e join e.labels l where l.metadataLabelId in :labelIds")
	List<String> findDistinctIdsByLabelsMetadataLabelIdIn(@Param("labelIds") Collection<String> labelIds);

	/**
	 * Overridden purely to attach {@code accessLabels} eagerly: it is lazy by default, and the label-move/-merge worker
	 * reads it on an already-detached entity (fetched in one transaction, persisted in the next), so it must come back
	 * populated with the page itself rather than needing a session that has since closed.
	 */
	@Override
	@EntityGraph(attributePaths = "accessLabels")
	List<ErrandEntity> findAllById(Iterable<String> ids);

	/**
	 * Locks every errand in {@code ids} with {@code SELECT ... FOR UPDATE}, ordered by id - used by the label-move
	 * runner, which restows a whole move inside one transaction and needs every affected errand locked against a
	 * concurrent write for the duration rather than retried against one. Ordered acquisition, combined with the
	 * runner chunking a globally-sorted id list, keeps lock acquisition ascending across the whole run, so it cannot
	 * deadlock against another ordered multi-row operation (a user PATCHing several errands, {@code ErrandPurgeRunner}'s
	 * own ascending-id walk).
	 * <p>
	 * No lock-timeout hint - left at the server/session default ({@code innodb_lock_wait_timeout}, 50s), deliberately:
	 * a brief conflict is worth waiting through rather than bouncing the run, and the default can be raised later,
	 * without a code change, if that ever proves too tight.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from ErrandEntity e where e.id in :ids order by e.id")
	List<ErrandEntity> findAllByIdForUpdate(@Param("ids") Collection<String> ids);

	// Used by the internal LabelMoveWorker helper's keyset-paged restow walk for a single moved label.
	@EntityGraph(attributePaths = "accessLabels")
	List<ErrandEntity> findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc(String metadataLabelId, String id, Pageable pageable);

	// Sibling of the single-id query above, for the label-merge worker walking several source labels at once - same
	// keyset paging, same reason for it.
	@EntityGraph(attributePaths = "accessLabels")
	List<ErrandEntity> findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc(Collection<String> metadataLabelIds, String id, Pageable pageable);

	/**
	 * The ids, among {@code ids}, of errands whose {@code labels} is non-empty - used by the label-move/-merge restow
	 * guard to tell "has labels" apart from "doesn't" for a page of already-detached entities, without needing
	 * {@code labels} itself eagerly fetched onto them: {@code labels} and {@code accessLabels} are both
	 * {@code @ElementCollection} bags, and Hibernate refuses to join-fetch two bags in the same query
	 * ({@code MultipleBagFetchException}), so this asks as a separate, cheap id-only query instead.
	 */
	@Query("select e.id from ErrandEntity e where e.id in :ids and e.labels is not empty")
	Set<String> findIdsWithNonEmptyLabels(@Param("ids") Collection<String> ids);

	boolean existsByPhasesPhaseEntityId(String phaseId);

	/**
	 * Puts {@code modified} and {@code touched} back to what they were before a label-move restow - a direct write
	 * rather than another {@code save}, since going through the entity again would only have {@code @PreUpdate} stamp
	 * both with {@code now()} a second time. A restow is driven by a label-tree change, not anything the errand's own
	 * occupant did, and must not reset the purge clock ({@code touched}) or reorder "recently modified" listings.
	 */
	@Modifying
	@Query("update ErrandEntity e set e.modified = :modified, e.touched = :touched where e.id = :id")
	void restoreModifiedAndTouched(@Param("id") String id, @Param("modified") OffsetDateTime modified, @Param("touched") OffsetDateTime touched);

}

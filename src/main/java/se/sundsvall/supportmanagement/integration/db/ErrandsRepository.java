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
	 * The ids, among {@code ids}, of errands whose {@code labels} is non-empty - used by the label-move/-merge restow
	 * guard to tell "has labels" apart from "doesn't" for a page of already-detached entities, without needing
	 * {@code labels} itself eagerly fetched onto them: {@code labels} and {@code accessLabels} are both
	 * {@code @ElementCollection} bags, and Hibernate refuses to join-fetch two bags in the same query
	 * ({@code MultipleBagFetchException}), so this asks as a separate, cheap id-only query instead.
	 */
	@Query("select e.id from ErrandEntity e where e.id in :ids and e.labels is not empty")
	Set<String> findIdsWithNonEmptyLabels(@Param("ids") Collection<String> ids);

	boolean existsByPhasesPhaseEntityId(String phaseId);

}

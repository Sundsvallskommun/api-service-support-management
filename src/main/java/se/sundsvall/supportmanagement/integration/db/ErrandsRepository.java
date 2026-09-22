package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
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

	boolean existsByPhasesPhaseEntityId(String phaseId);

}

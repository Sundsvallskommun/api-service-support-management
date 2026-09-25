package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
	 * {@code LabelMoveWorker#restowErrands} for what went wrong when the walk instead re-derived its own, narrower set
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

	boolean existsByPhasesPhaseEntityId(String phaseId);

}

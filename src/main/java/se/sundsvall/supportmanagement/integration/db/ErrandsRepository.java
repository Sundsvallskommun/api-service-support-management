package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

	/**
	 * The ids of every errand tagged with any of the given label ids - distinct, since an errand tagged with more than
	 * one of them (e.g. the moved/merged label itself and one of its descendants, both stored per the
	 * ancestor-chain-expansion invariant) must still count once.
	 * <p>
	 * The single source of truth for "which errands does this operation affect": a restructure dry run reads its
	 * per-step {@code affectedErrandCount} from {@code size()} here, and the job this starts resolves the same union
	 * once at the outset and carries it through {@code LabelRestructureRun} so the job's own {@code total} and the
	 * walk that restows them read from the exact same set - see the review discussion on the former
	 * {@code LabelMoveRunner#restowErrands} for what went wrong when the walk instead re-derived its own, narrower set
	 * from a single label id.
	 */
	@Query("select distinct e.id from ErrandEntity e join e.labels l where l.metadataLabelId in :labelIds")
	List<String> findDistinctIdsByLabelsMetadataLabelIdIn(@Param("labelIds") Collection<String> labelIds);

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

	/**
	 * The ids of the next batch of a keyset walk over the errands the specification reaches: in ascending id order,
	 * starting after the id the previous batch ended on, and no more than the limit. Only the ids are read.
	 * <p>
	 *
	 * @param  specification the errands the walk reaches
	 * @param  cursor        the id the previous batch ended on, or null for the first batch
	 * @param  limit         the most ids to return
	 * @return               the ids of the next batch, empty when the walk has reached its end
	 */
	default List<String> findIdsAfter(final Specification<ErrandEntity> specification, final String cursor, final int limit) {
		final var idAttribute = "id";
		final var reached = Optional.ofNullable(cursor)
			.map(id -> specification.and((root, _, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get(idAttribute), id)))
			.orElse(specification);

		return findBy(reached, query -> query
			.project(idAttribute)
			.sortBy(Sort.by(ASC, idAttribute))
			.limit(limit)
			.as(IdProjection.class)
			.all())
			.stream()
			.map(IdProjection::getId)
			.toList();
	}
}

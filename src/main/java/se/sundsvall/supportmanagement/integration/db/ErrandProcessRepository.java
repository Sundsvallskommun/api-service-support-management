package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;

@Transactional
@CircuitBreaker(name = "errandProcessRepository")
public interface ErrandProcessRepository extends JpaRepository<ErrandProcessEntity, String> {

	/**
	 * The live instance of an errand, if it has one.
	 * <p>
	 * At most one row can answer, and that is held by {@code uq_ep_one_active_per_errand} rather than by this query.
	 *
	 * @param  errandId the errand to look at.
	 * @return          the live instance, or empty when the errand has none.
	 */
	Optional<ErrandProcessEntity> findByErrandIdAndActiveMarkerIsNotNull(String errandId);

	/**
	 * The row a report from the process engine belongs to. Unique by {@code uq_ep_process_instance_id}.
	 *
	 * @param  processInstanceId the instance the process engine reports on.
	 * @return                   the row for the instance, or empty when SM has never seen it.
	 */
	Optional<ErrandProcessEntity> findByProcessInstanceId(String processInstanceId);

	/**
	 * Every instance an errand has had, newest first. Normally exactly one.
	 *
	 * @param  errandId       the errand to look at.
	 * @param  municipalityId the municipality the errand belongs to.
	 * @param  namespace      the namespace the errand belongs to.
	 * @param  pageable       the page to read.
	 * @return                the instances of the errand, newest first.
	 */
	List<ErrandProcessEntity> findByErrandIdAndMunicipalityIdAndNamespaceOrderByCreatedDesc(String errandId, String municipalityId, String namespace, Pageable pageable);

	/**
	 * Whether an errand has ever had an instance reach the sent in state. Asked about COMPLETED to keep a process that has
	 * run its course from being started over by an ordinary errand change.
	 *
	 * @param  errandId      the errand to look at.
	 * @param  processStatus the state to look for.
	 * @return               whether the errand has an instance in that state.
	 */
	boolean existsByErrandIdAndProcessStatus(String errandId, ProcessStatus processStatus);

	/**
	 * Whether an errand already has an instance of some other process than the sent in one. All instances of an errand run
	 * the same process model, and that is the one rule of the three about a single process per errand that the database
	 * cannot hold on its own.
	 *
	 * @param  errandId   the errand to look at.
	 * @param  processKey the process model the incoming report claims.
	 * @return            whether the errand already runs a different process.
	 */
	boolean existsByErrandIdAndProcessKeyNot(String errandId, String processKey);

	/**
	 * The instances of a whole page of errands, newest first.
	 * <p>
	 * One query for the page rather than one per errand, which is what the {@code process} projection on the errand is
	 * built from: the caller keeps the first row it sees per errand, and the ordering makes that the latest one.
	 * <p>
	 * Scoped by namespace and municipality like every other read of this table, so that a caller holding errand ids from
	 * somewhere else cannot reach across a tenant by handing them over.
	 *
	 * @param  errandIds      the errands to read the instances of.
	 * @param  municipalityId the municipality of the errands.
	 * @param  namespace      the namespace of the errands.
	 * @return                the instances of those errands, newest first.
	 */
	List<ErrandProcessEntity> findByErrandIdInAndMunicipalityIdAndNamespaceOrderByCreatedDesc(Collection<String> errandIds, String municipalityId, String namespace);
}

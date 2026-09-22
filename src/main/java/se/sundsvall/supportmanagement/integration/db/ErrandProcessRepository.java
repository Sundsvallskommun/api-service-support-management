package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;

@Transactional
@CircuitBreaker(name = "errandProcessRepository")
public interface ErrandProcessRepository extends JpaRepository<ErrandProcessEntity, String> {

	/**
	 * The live instance of an errand, if it has one.
	 * <p>
	 * At most one row can answer, which the unique key {@code uq_ep_one_active_per_errand} guarantees.
	 *
	 * @param  errandId the errand to look at.
	 * @return          the live instance, or empty when the errand has none.
	 */
	Optional<ErrandProcessEntity> findByErrandIdAndActiveMarkerIsNotNull(String errandId);

	/**
	 * Every process row of an errand, live or finished, newest first. Normally exactly one.
	 * <p>
	 * One read for every question asked of the process history of an errand: which process it runs, whether a live
	 * instance stands in the way, and whether its process life is over.
	 *
	 * @param  errandId the errand to look at.
	 * @return          the process rows of the errand, newest first, and empty for one that has never had a process.
	 */
	List<ErrandProcessEntity> findByErrandIdOrderByCreatedDesc(String errandId);

	/**
	 * The row a report from the process engine belongs to. Unique by {@code uq_ep_process_instance_id}.
	 *
	 * @param  processInstanceId the instance the process engine reports on.
	 * @return                   the row for the instance, or empty when SM has never seen it.
	 */
	Optional<ErrandProcessEntity> findByProcessInstanceId(String processInstanceId);

	/**
	 * The row of an instance, provided it belongs to the errand asked about.
	 *
	 * @param  processInstanceId the instance to look up.
	 * @param  errandId          the errand the instance has to belong to.
	 * @return                   the row for the instance, or empty when SM has never seen it or it belongs to another
	 *                           errand.
	 */
	Optional<ErrandProcessEntity> findByProcessInstanceIdAndErrandId(String processInstanceId, String errandId);

	/**
	 * The instances of a whole page of errands, newest first.
	 * <p>
	 * The {@code process} projection on the errand is built from this: the caller keeps the first row it sees per errand,
	 * and the ordering makes that the latest one.
	 * <p>
	 * Scoped by namespace and municipality, so errand ids of another namespace or municipality find nothing.
	 *
	 * @param  errandIds      the errands to read the instances of.
	 * @param  municipalityId the municipality of the errands.
	 * @param  namespace      the namespace of the errands.
	 * @return                the instances of those errands, newest first.
	 */
	List<ErrandProcessEntity> findByErrandIdInAndMunicipalityIdAndNamespaceOrderByCreatedDesc(Collection<String> errandIds, String municipalityId, String namespace);
}

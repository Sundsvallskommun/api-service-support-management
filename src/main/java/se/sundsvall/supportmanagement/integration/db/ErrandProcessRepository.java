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
	 * At most one row can answer, and that is held by {@code uq_ep_one_active_per_errand} rather than by this query.
	 *
	 * @param  errandId the errand to look at.
	 * @return          the live instance, or empty when the errand has none.
	 */
	Optional<ErrandProcessEntity> findByErrandIdAndActiveMarkerIsNotNull(String errandId);

	/**
	 * Every process row of an errand, live or finished, newest first. Normally exactly one.
	 * <p>
	 * One read for every question asked of the process history of an errand: which process it runs, whether a live
	 * instance stands in the way, and whether its process life is over. Reading only the live one would leave the last
	 * question unanswered - a completed process ends the process life of an errand, and a completed row is not a live
	 * one. An errand holds a handful of rows at most, so the questions are answered from the rows rather than by a query
	 * each.
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
	 * The instances of a whole page of errands, newest first.
	 * <p>
	 * One query for the page rather than one per errand, which is what the {@code process} projection on the errand is
	 * built from: the caller keeps the first row it sees per errand, and the ordering makes that the latest one.
	 * <p>
	 * Scoped by namespace and municipality, so that a caller holding errand ids from somewhere else cannot reach across a
	 * tenant by handing them over.
	 *
	 * @param  errandIds      the errands to read the instances of.
	 * @param  municipalityId the municipality of the errands.
	 * @param  namespace      the namespace of the errands.
	 * @return                the instances of those errands, newest first.
	 */
	List<ErrandProcessEntity> findByErrandIdInAndMunicipalityIdAndNamespaceOrderByCreatedDesc(Collection<String> errandIds, String municipalityId, String namespace);
}

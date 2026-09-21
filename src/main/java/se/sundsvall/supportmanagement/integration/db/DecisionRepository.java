package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;

/**
 * Reads the decisions of an errand through its errand.
 * <p>
 * Every lookup names the namespace, the municipality and the errand. Asking for an artefact of another errand finds
 * nothing, and the caller turns that into a 404.
 */
@CircuitBreaker(name = "decisionRepository")
public interface DecisionRepository extends JpaRepository<DecisionEntity, String> {

	Optional<DecisionEntity> findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(String namespace, String municipalityId, String errandId, String id);

	List<DecisionEntity> findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(String namespace, String municipalityId, String errandId);

	boolean existsByNamespaceAndMunicipalityIdAndErrandEntityId(String namespace, String municipalityId, String errandId);

	/**
	 * Whether a decision of the errand has the attachment linked to it.
	 *
	 * @param  namespace      namespace of the errand.
	 * @param  municipalityId municipality of the errand.
	 * @param  errandId       the errand the decision and the attachment belong to.
	 * @param  attachmentId   the attachment to look for.
	 * @return                true when at least one decision of the errand links it.
	 */
	boolean existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsId(String namespace, String municipalityId, String errandId, String attachmentId);

	/**
	 * Whether a decision of the errand in the given status has the attachment linked to it.
	 *
	 * @param  namespace      namespace of the errand.
	 * @param  municipalityId municipality of the errand.
	 * @param  errandId       the errand the decision and the attachment belong to.
	 * @param  attachmentId   the attachment to look for.
	 * @param  status         the status the linking decision has to have.
	 * @return                true when at least one such decision links it.
	 */
	boolean existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsIdAndStatus(String namespace, String municipalityId, String errandId, String attachmentId, ItemStatus status);

	/**
	 * Whether a decision of the errand rests on the investigation.
	 *
	 * @param  namespace       namespace of the errand.
	 * @param  municipalityId  municipality of the errand.
	 * @param  errandId        the errand the decision and the investigation belong to.
	 * @param  investigationId the investigation to look for.
	 * @return                 true when at least one decision of the errand rests on it.
	 */
	boolean existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityId(String namespace, String municipalityId, String errandId, String investigationId);

	/**
	 * Whether a decision of the errand in the given status rests on the investigation.
	 *
	 * @param  namespace       namespace of the errand.
	 * @param  municipalityId  municipality of the errand.
	 * @param  errandId        the errand the decision and the investigation belong to.
	 * @param  investigationId the investigation to look for.
	 * @param  status          the status the decision resting on it has to have.
	 * @return                 true when at least one such decision rests on it.
	 */
	boolean existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityIdAndStatus(String namespace, String municipalityId, String errandId, String investigationId, ItemStatus status);
}

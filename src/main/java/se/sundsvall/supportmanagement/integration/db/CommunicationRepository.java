package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.IdProjection;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationEntity;

@CircuitBreaker(name = "communicationRepository")
public interface CommunicationRepository extends JpaRepository<CommunicationEntity, String> {

	List<CommunicationEntity> findByErrandNumberAndNamespaceAndMunicipalityId(String errandNumber, String namespace, String municipalityId);

	/**
	 * Find the ids of the communications of an errand. Only the ids are read, not the communications themselves.
	 *
	 * @param  errandNumber   number of the errand.
	 * @param  namespace      namespace of the errand.
	 * @param  municipalityId id of the municipality of the errand.
	 * @return                the ids of the communications of the errand.
	 */
	List<IdProjection> findIdsByErrandNumberAndNamespaceAndMunicipalityId(String errandNumber, String namespace, String municipalityId);

	List<CommunicationEntity> findByErrandNumberAndNamespaceAndMunicipalityIdAndInternal(String errandNumber, String namespace, String municipalityId, boolean isInternal);

	boolean existsByErrandNumberAndNamespaceAndMunicipalityIdAndExternalId(String errandNumber, String namespace, String municipalityId, String externalId);

	Optional<CommunicationEntity> findByIdAndErrandNumberAndNamespaceAndMunicipalityId(String id, String errandNumber, String namespace, String municipalityId);
}

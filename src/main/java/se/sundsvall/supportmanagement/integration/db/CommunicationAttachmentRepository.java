package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationAttachmentEntity;

@CircuitBreaker(name = "communicationAttachmentRepository")
public interface CommunicationAttachmentRepository extends JpaRepository<CommunicationAttachmentEntity, String> {

	Optional<CommunicationAttachmentEntity> findByNamespaceAndMunicipalityIdAndCommunicationEntityIdAndId(final String namespace, final String municipalityId, final String communicationId, final String id);
}

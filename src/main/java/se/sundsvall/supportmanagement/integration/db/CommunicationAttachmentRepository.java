package se.sundsvall.supportmanagement.integration.db;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;

@CircuitBreaker(name = "communicationAttachmentRepository")
public interface CommunicationAttachmentRepository extends JpaRepository<CommunicationAttachmentEntity, String> {

	Optional<CommunicationAttachmentEntity> findByNamespaceAndMunicipalityIdAndId(final String namespace, final String municipalityId, final String id);
}

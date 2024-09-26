package se.sundsvall.supportmanagement.integration.db;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@CircuitBreaker(name = "communicationAttachmentRepository")
public interface CommunicationAttachmentRepository extends JpaRepository<CommunicationAttachmentEntity, String> {

	Optional<CommunicationAttachmentEntity> findByNamespaceAndMunicipalityIdAndId(final String namespace, final String municipalityId, final String id);
	
}

package se.sundsvall.supportmanagement.integration.db;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@CircuitBreaker(name = "attachmentRepository")
public interface AttachmentRepository extends JpaRepository<AttachmentEntity, String> {

	Optional<AttachmentEntity> findByNamespaceAndMunicipalityIdAndId(final String namespace, final String municipalityId, final String id);

	List<AttachmentEntity> findByNamespaceAndMunicipalityIdAndIdIn(final String namespace, final String municipalityId, final List<String> ids);

}

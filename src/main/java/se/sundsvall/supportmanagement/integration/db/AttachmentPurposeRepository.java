package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentPurposeEntity;

@Transactional
@CircuitBreaker(name = "attachmentPurposeRepository")
public interface AttachmentPurposeRepository extends JpaRepository<AttachmentPurposeEntity, String> {

	List<AttachmentPurposeEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId, Sort sort);

	boolean existsByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	Optional<AttachmentPurposeEntity> findByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	AttachmentPurposeEntity getByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	void deleteByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);
}

package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;

@CircuitBreaker(name = "attachmentRepository")
public interface AttachmentRepository extends JpaRepository<AttachmentEntity, String> {

	Optional<AttachmentEntity> findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(final String namespace, final String municipalityId, final String errandId, final String id);

	List<AttachmentEntity> findByNamespaceAndMunicipalityIdAndIdIn(final String namespace, final String municipalityId, final List<String> ids);

	@Query("SELECT a.id FROM AttachmentEntity a WHERE a.hash IS NULL")
	List<String> findIdsByHashIsNull(Pageable pageable);

	List<AttachmentEntity> findByNamespaceAndMunicipalityIdAndErrandEntityIdAndIdIn(final String namespace, final String municipalityId, final String errandId, final List<String> ids);

}

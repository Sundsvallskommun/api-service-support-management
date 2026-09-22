package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataIdProjection;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.IdProjection;

@CircuitBreaker(name = "attachmentRepository")
public interface AttachmentRepository extends JpaRepository<AttachmentEntity, String> {

	Optional<AttachmentEntity> findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(final String namespace, final String municipalityId, final String errandId, final String id);

	List<IdProjection> findByHashIsNull(Pageable pageable);

	/**
	 * Find the ids of the data rows the sent in attachments point at.
	 * <p>
	 * Only those ids are read, without loading the files, so a removal can name the data rows instead of reaching them
	 * through the cascade from the attachment.
	 *
	 * @param  ids ids of the attachments.
	 * @return     the ids of the data rows they point at.
	 */
	List<AttachmentDataIdProjection> findByIdIn(List<String> ids);

	boolean existsByPurposeId(String purposeId);

	List<AttachmentEntity> findByNamespaceAndMunicipalityIdAndErrandEntityIdAndIdIn(final String namespace, final String municipalityId, final String errandId, final List<String> ids);

}

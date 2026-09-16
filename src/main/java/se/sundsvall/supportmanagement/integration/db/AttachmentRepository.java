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
	 * Only those ids are read, and that is the point of the method. An attachment cascades its removal onto its data,
	 * and cascading means loading - which for a data row means the whole file in the heap. An errand carrying scanned
	 * documents holds more of them than there is heap to load them into, so a removal names the rows instead of
	 * reaching them.
	 *
	 * @param  ids ids of the attachments.
	 * @return     the ids of the data rows they point at.
	 */
	List<AttachmentDataIdProjection> findByIdIn(List<String> ids);

	List<AttachmentEntity> findByNamespaceAndMunicipalityIdAndErrandEntityIdAndIdIn(final String namespace, final String municipalityId, final String errandId, final List<String> ids);

}

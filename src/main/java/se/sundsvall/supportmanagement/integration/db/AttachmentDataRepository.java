package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;

/**
 * The blobs attachments are made of.
 * <p>
 * Reached directly only to remove rows by id, which does not load the file. A data row is otherwise always reached
 * through the attachment that points at it, and is normally removed by the cascade on that association.
 */
@CircuitBreaker(name = "attachmentDataRepository")
public interface AttachmentDataRepository extends JpaRepository<AttachmentDataEntity, Integer> {
}

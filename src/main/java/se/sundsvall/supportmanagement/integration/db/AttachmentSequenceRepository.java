package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentSequenceEntity;

@CircuitBreaker(name = "attachmentSequenceRepository")
public interface AttachmentSequenceRepository extends JpaRepository<AttachmentSequenceEntity, String> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<AttachmentSequenceEntity> findByErrandId(String errandId);
}

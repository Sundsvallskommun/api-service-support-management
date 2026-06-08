package se.sundsvall.supportmanagement.integration.db;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.HandoverIdempotencyEntity;

public interface HandoverIdempotencyRepository extends JpaRepository<HandoverIdempotencyEntity, String> {

	Optional<HandoverIdempotencyEntity> findByIdempotencyKeyAndExpiresAtAfter(String idempotencyKey, OffsetDateTime now);

}

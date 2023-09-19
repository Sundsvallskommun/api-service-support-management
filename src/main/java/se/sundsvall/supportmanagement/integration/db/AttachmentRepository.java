package se.sundsvall.supportmanagement.integration.db;

import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;

import java.util.Optional;


public interface AttachmentRepository extends JpaRepository<AttachmentEntity, String> {

	Optional<AttachmentEntity> findById(String id);
}

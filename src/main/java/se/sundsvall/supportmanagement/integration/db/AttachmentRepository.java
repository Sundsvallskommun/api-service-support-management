package se.sundsvall.supportmanagement.integration.db;

import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;


public interface AttachmentRepository extends JpaRepository<AttachmentEntity, String> {
}

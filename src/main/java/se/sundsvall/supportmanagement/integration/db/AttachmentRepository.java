package se.sundsvall.supportmanagement.integration.db;

import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;

import javax.transaction.Transactional;
import java.util.List;

@Transactional
public interface AttachmentRepository extends JpaRepository<AttachmentEntity, String> {

	List<AttachmentEntity> findByErrandEntityId(String id);
}

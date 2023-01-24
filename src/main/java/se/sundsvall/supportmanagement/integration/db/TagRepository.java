package se.sundsvall.supportmanagement.integration.db;

import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.TagEntity;
import se.sundsvall.supportmanagement.integration.db.model.TagType;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Transactional
public interface TagRepository extends JpaRepository<TagEntity, Long> {

	Optional<TagEntity> findByNameIgnoreCase(String name);

	List<TagEntity> findByType(TagType type);
}

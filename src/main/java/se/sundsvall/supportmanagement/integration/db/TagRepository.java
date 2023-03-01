package se.sundsvall.supportmanagement.integration.db;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.sundsvall.supportmanagement.integration.db.model.TagEntity;
import se.sundsvall.supportmanagement.integration.db.model.TagType;

@Transactional
@CircuitBreaker(name = "tagRepository")
public interface TagRepository extends JpaRepository<TagEntity, Long> {

	Optional<TagEntity> findByNameIgnoreCase(String name);

	List<TagEntity> findByType(TagType type);
}

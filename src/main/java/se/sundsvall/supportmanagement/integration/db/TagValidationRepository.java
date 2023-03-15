package se.sundsvall.supportmanagement.integration.db;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.sundsvall.supportmanagement.integration.db.model.TagValidationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.TagType;

@Transactional
@CircuitBreaker(name = "tagValidationRepository")
public interface TagValidationRepository extends JpaRepository<TagValidationEntity, Long> {
	Optional<TagValidationEntity> findByNamespaceAndMunicipalityIdAndType(String namespace, String municipalityId, TagType type);
	List<TagValidationEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);
}

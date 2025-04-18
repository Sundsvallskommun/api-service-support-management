package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.ValidationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;

@Transactional
@CircuitBreaker(name = "validationRepository")
public interface ValidationRepository extends JpaRepository<ValidationEntity, Long> {
	Optional<ValidationEntity> findByNamespaceAndMunicipalityIdAndType(String namespace, String municipalityId, EntityType type);

	List<ValidationEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);
}

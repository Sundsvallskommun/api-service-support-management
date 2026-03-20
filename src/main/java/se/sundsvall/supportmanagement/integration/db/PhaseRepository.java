package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.PhaseEntity;

@Transactional
@CircuitBreaker(name = "phaseRepository")
public interface PhaseRepository extends JpaRepository<PhaseEntity, String> {

	List<PhaseEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	Optional<PhaseEntity> findByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	boolean existsByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	void deleteByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);
}

package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;

import java.util.Optional;

@Transactional
@CircuitBreaker(name = "NamespaceConfigRepository")
public interface NamespaceConfigRepository extends JpaRepository<NamespaceConfigEntity, Long> {
	Optional<NamespaceConfigEntity> getByNamespaceAndMunicipalityId(String namespace, String municipalityId);
	boolean existsByNamespaceAndMunicipalityId(String namespace, String municipalityId);
	void deleteByNamespaceAndMunicipalityId(String namespace, String municipalityId);
}

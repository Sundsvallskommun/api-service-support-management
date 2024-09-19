package se.sundsvall.supportmanagement.integration.db;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;

@Transactional
@CircuitBreaker(name = "NamespaceConfigRepository")
public interface NamespaceConfigRepository extends JpaRepository<NamespaceConfigEntity, Long> {
	Optional<NamespaceConfigEntity> getByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	List<NamespaceConfigEntity> findAllByMunicipalityId(String municipalityId);

	boolean existsByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	void deleteByNamespaceAndMunicipalityId(String namespace, String municipalityId);
}

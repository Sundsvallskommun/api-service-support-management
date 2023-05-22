package se.sundsvall.supportmanagement.integration.db;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;

@Transactional
@CircuitBreaker(name = "statusRepository")
public interface StatusRepository extends JpaRepository<StatusEntity, Long> {
	List<StatusEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	boolean existsByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	StatusEntity getByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	void deleteByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);
}

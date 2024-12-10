package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeEntity;

@Transactional
@CircuitBreaker(name = "externalIdTypeRepository")
public interface ExternalIdTypeRepository extends JpaRepository<ExternalIdTypeEntity, Long> {
	List<ExternalIdTypeEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	boolean existsByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	ExternalIdTypeEntity getByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	void deleteByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);
}

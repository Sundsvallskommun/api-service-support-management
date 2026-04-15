package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;

@Transactional
@CircuitBreaker(name = "statusRepository")
public interface StatusRepository extends JpaRepository<StatusEntity, String> {
	List<StatusEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	boolean existsByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	StatusEntity getByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	void deleteByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);
}

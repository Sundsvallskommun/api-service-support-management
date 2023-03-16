package se.sundsvall.supportmanagement.integration.db;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeEntity;

@Transactional
@CircuitBreaker(name = "externalIdTypeRepository")
public interface ExternalIdTypeRepository extends JpaRepository<ExternalIdTypeEntity, Long> {
	List<ExternalIdTypeEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);
}

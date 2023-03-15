package se.sundsvall.supportmanagement.integration.db;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeTagEntity;

@Transactional
@CircuitBreaker(name = "externalIdTypeTagRepository")
public interface ExternalIdTypeTagRepository extends JpaRepository<ExternalIdTypeTagEntity, Long> {
	List<ExternalIdTypeTagEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);
}

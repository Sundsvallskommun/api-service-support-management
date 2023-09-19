package se.sundsvall.supportmanagement.integration.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.sundsvall.supportmanagement.integration.db.model.LabelEntity;

@Transactional
@CircuitBreaker(name = "labelRepository")
public interface LabelRepository extends JpaRepository<LabelEntity, Long> {
	LabelEntity findOneByNamespaceAndMunicipalityId(String namespace, String municipalityId);
}

package se.sundsvall.supportmanagement.integration.db;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.sundsvall.supportmanagement.integration.db.model.StatusTagEntity;

@Transactional
@CircuitBreaker(name = "statusTagRepository")
public interface StatusTagRepository extends JpaRepository<StatusTagEntity, Long> {
	List<StatusTagEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);
}

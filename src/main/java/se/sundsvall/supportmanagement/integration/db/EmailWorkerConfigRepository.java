package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.EmailWorkerConfigEntity;

@Transactional
@CircuitBreaker(name = "emailWorkerConfigRepository")
public interface EmailWorkerConfigRepository extends JpaRepository<EmailWorkerConfigEntity, Long> {
	Optional<EmailWorkerConfigEntity> getByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	void deleteByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	boolean existsByNamespaceAndMunicipalityId(String namespace, String municipalityId);
}

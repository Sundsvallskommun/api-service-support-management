package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.JobEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.JobStatus;

@Transactional
@CircuitBreaker(name = "jobRepository")
public interface JobRepository extends JpaRepository<JobEntity, String> {

	Optional<JobEntity> findByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	boolean existsByNamespaceAndMunicipalityIdAndStatusIn(String namespace, String municipalityId, Collection<JobStatus> statuses);
}

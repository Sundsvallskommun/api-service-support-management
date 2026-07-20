package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import se.sundsvall.supportmanagement.integration.db.model.ErrandNumberSequenceEntity;

@CircuitBreaker(name = "errandNumberSequenceRepository")
public interface ErrandNumberSequenceRepository extends JpaRepository<ErrandNumberSequenceEntity, String> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ErrandNumberSequenceEntity> findByNamespaceAndMunicipalityId(String namespace, String municipalityId);

}

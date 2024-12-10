package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandNumberSequenceEntity;

@CircuitBreaker(name = "errandNumberSequenceRepository")
public interface ErrandNumberSequenceRepository extends JpaRepository<ErrandNumberSequenceEntity, String> {

	Optional<ErrandNumberSequenceEntity> findByNamespaceAndMunicipalityId(String namespace, String municipalityId);

}

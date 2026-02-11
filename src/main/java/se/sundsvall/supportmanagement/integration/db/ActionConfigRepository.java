package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;

@Transactional
@CircuitBreaker(name = "actionConfigRepository")
public interface ActionConfigRepository extends JpaRepository<ActionConfigEntity, String> {

	List<ActionConfigEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	Optional<ActionConfigEntity> findByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	void deleteByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);
}

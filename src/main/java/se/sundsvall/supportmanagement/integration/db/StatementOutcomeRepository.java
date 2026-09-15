package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.StatementOutcomeEntity;

@Transactional
@CircuitBreaker(name = "statementOutcomeRepository")
public interface StatementOutcomeRepository extends JpaRepository<StatementOutcomeEntity, String> {

	List<StatementOutcomeEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId, Sort sort);

	boolean existsByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	boolean existsByNamespaceAndMunicipalityIdAndNameAndIdNot(String namespace, String municipalityId, String name, String id);

	Optional<StatementOutcomeEntity> findByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	StatementOutcomeEntity getByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	void deleteByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);
}

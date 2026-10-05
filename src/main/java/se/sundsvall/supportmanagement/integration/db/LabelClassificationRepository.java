package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.LabelClassificationEntity;

@Transactional
@CircuitBreaker(name = "labelClassificationRepository")
public interface LabelClassificationRepository extends JpaRepository<LabelClassificationEntity, String> {

	List<LabelClassificationEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId, Sort sort);

	Optional<LabelClassificationEntity> findByNamespaceAndMunicipalityIdAndClassification(String namespace, String municipalityId, String classification);

	boolean existsByNamespaceAndMunicipalityIdAndClassification(String namespace, String municipalityId, String classification);

	void deleteByNamespaceAndMunicipalityIdAndClassification(String namespace, String municipalityId, String classification);
}

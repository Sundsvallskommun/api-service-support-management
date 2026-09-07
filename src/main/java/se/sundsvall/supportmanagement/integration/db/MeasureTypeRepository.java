package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.MeasureTypeEntity;

@Transactional
@CircuitBreaker(name = "measureTypeRepository")
public interface MeasureTypeRepository extends JpaRepository<MeasureTypeEntity, String> {

	List<MeasureTypeEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId, Sort sort);

	List<MeasureTypeEntity> findAllByNamespaceAndMunicipalityIdAndMeasureGroup(String namespace, String municipalityId, String measureGroup, Sort sort);

	boolean existsByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	MeasureTypeEntity getByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	void deleteByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);
}

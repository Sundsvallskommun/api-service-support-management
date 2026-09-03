package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

	// Shared by measure validation and metadata writes to prevent deletion during a measure write.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<MeasureTypeEntity> findWithLockingByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<MeasureTypeEntity> findWithLockingByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

}

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

	/**
	 * Reads the type under a lock shared with other readers, held until the transaction ends.
	 * <p>
	 * A measure write takes this lock while it validates its type, so that the catalogue cannot delete the type between
	 * that validation and the commit of the measure: a catalogue write takes the exclusive lock below and waits until the
	 * measure is committed, after which its reference check sees the new measure. Readers do not block each other, so
	 * measure writes referring to the same type run concurrently and cannot deadlock on the order they lock types in.
	 */
	@Lock(LockModeType.PESSIMISTIC_READ)
	Optional<MeasureTypeEntity> findWithSharedLockByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	/**
	 * Reads the type under an exclusive lock, held until the transaction ends. Catalogue writes take it so that the
	 * reference check they make next cannot race a measure write validating the same type, see
	 * {@link #findWithSharedLockByNamespaceAndMunicipalityIdAndName}.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<MeasureTypeEntity> findWithLockingByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);
}

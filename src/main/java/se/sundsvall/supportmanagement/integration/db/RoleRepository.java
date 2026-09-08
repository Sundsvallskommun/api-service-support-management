package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.RoleEntity;

@Transactional
@CircuitBreaker(name = "roleRepository")
public interface RoleRepository extends JpaRepository<RoleEntity, String> {

	@Lock(LockModeType.PESSIMISTIC_READ)
	Optional<RoleEntity> findByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<RoleEntity> findWithLockingByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	List<RoleEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId, Sort sort);

	boolean existsByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	RoleEntity getByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	void deleteByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);
}

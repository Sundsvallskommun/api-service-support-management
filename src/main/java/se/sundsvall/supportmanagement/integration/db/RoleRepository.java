package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.RoleEntity;

@Transactional
@CircuitBreaker(name = "roleRepository")
public interface RoleRepository extends JpaRepository<RoleEntity, String> {

	List<RoleEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId, Sort sort);

	boolean existsByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	RoleEntity getByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	void deleteByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);
}

package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;

import javax.transaction.Transactional;
import java.util.List;

@Transactional
@CircuitBreaker(name = "categoryRepository")
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
	List<CategoryEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	boolean existsByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	CategoryEntity getByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	void deleteByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);
}

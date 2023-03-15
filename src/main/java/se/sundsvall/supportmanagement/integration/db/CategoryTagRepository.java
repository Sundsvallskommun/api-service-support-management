package se.sundsvall.supportmanagement.integration.db;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.sundsvall.supportmanagement.integration.db.model.CategoryTagEntity;

@Transactional
@CircuitBreaker(name = "categoryTagRepository")
public interface CategoryTagRepository extends JpaRepository<CategoryTagEntity, Long> {
	List<CategoryTagEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);
}

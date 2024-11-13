package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.WebMessageCollectEntity;

import java.util.List;

@CircuitBreaker(name = "webMessageCollectRepository")
public interface WebMessageCollectRepository extends JpaRepository<WebMessageCollectEntity, Long> {

	List<WebMessageCollectEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);
}

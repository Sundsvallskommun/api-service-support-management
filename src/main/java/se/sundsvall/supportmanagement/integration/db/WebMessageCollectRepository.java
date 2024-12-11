package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.WebMessageCollectEntity;

@CircuitBreaker(name = "webMessageCollectRepository")
public interface WebMessageCollectRepository extends JpaRepository<WebMessageCollectEntity, Long> {

	List<WebMessageCollectEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);
}

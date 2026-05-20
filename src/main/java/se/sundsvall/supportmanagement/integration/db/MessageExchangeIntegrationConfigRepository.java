package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeIntegrationConfigEntity;

@Transactional
@CircuitBreaker(name = "messageExchangeIntegrationConfigRepository")
public interface MessageExchangeIntegrationConfigRepository extends JpaRepository<MessageExchangeIntegrationConfigEntity, Long> {

	Optional<MessageExchangeIntegrationConfigEntity> getByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	void deleteByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	boolean existsByNamespaceAndMunicipalityId(String namespace, String municipalityId);
}

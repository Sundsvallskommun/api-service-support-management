package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeSyncEntity;

@CircuitBreaker(name = "messageExchangeSyncRepository")
public interface MessageExchangeSyncRepository extends JpaRepository<MessageExchangeSyncEntity, Long> {

	List<MessageExchangeSyncEntity> findByActive(Boolean active);

	List<MessageExchangeSyncEntity> findByMunicipalityId(String municipalityId);

	Optional<MessageExchangeSyncEntity> findByIdAndMunicipalityId(Long id, String municipalityId);
}

package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeSyncEntity;

@CircuitBreaker(name = "messageExchangeSyncRepository")
public interface MessageExchangeSyncRepository extends JpaRepository<MessageExchangeSyncEntity, Long> {

	List<MessageExchangeSyncEntity> findByActive(Boolean active);
}

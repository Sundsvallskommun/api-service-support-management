package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;

@CircuitBreaker(name = "communicationRepository")
public interface CommunicationRepository extends JpaRepository<CommunicationEntity, String> {

	List<CommunicationEntity> findByErrandNumber(String errandNumber);

	boolean existsByErrandNumberAndExternalId(String errandNumber, String externalId);
}

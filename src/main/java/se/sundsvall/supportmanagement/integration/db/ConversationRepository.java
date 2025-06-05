package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.communication.ConversationEntity;

@CircuitBreaker(name = "conversationRepository")
public interface ConversationRepository extends JpaRepository<ConversationEntity, String> {

	List<ConversationEntity> findByMunicipalityIdAndNamespaceAndErrandId(String municipalityId, String namespace, String errandId);

	Optional<ConversationEntity> findByMunicipalityIdAndNamespaceAndErrandIdAndId(String municipalityId, String namespace, String errandId, final String id);

	Optional<ConversationEntity> findByMessageExchangeId(String messageExchangeId);
}

package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.supportmanagement.integration.db.model.SubscriberNotificationEntity;

@CircuitBreaker(name = "subscriberNotificationRepository")
public interface SubscriberNotificationRepository extends JpaRepository<SubscriberNotificationEntity, String> {

	@Query("""
		SELECT n FROM SubscriberNotificationEntity n
		WHERE n.municipalityId = :municipalityId
		AND n.namespace = :namespace
		AND n.identifierType = :identifierType
		AND n.identifierValue = :identifierValue
		AND (n.expires IS NULL OR n.expires > :now)
		""")
	Page<SubscriberNotificationEntity> findActiveByMunicipalityIdAndNamespaceAndIdentifierTypeAndIdentifierValue(
		@Param("municipalityId") String municipalityId,
		@Param("namespace") String namespace,
		@Param("identifierType") String identifierType,
		@Param("identifierValue") String identifierValue,
		@Param("now") OffsetDateTime now,
		Pageable pageable);

	Optional<SubscriberNotificationEntity> findByIdAndMunicipalityIdAndNamespace(
		String id, String municipalityId, String namespace);

	/**
	 * Removes every notification held for an errand. Each notification takes its events with it through the cascade on
	 * the notification, which is why the entities are loaded rather than deleted in bulk.
	 *
	 * @param  errandId the id of the errand the notifications belong to.
	 * @return          the number of removed notifications.
	 */
	long deleteAllByErrandId(String errandId);
}

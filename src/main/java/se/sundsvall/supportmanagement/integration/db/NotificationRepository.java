package se.sundsvall.supportmanagement.integration.db;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;

@CircuitBreaker(name = "notificationRepository")
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

	boolean existsByNamespaceAndMunicipalityIdAndOwnerIdAndErrandEntityAndDescriptionAndCreatedIsAfter(
		final String namespace,
		final String municipalityId,
		final String ownerId,
		final ErrandEntity errandEntity,
		final String description,
		final OffsetDateTime created);

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	Optional<NotificationEntity> findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(String id, String namespace, String municipalityId, String errandId);

	List<NotificationEntity> findAllByNamespaceAndMunicipalityIdAndOwnerId(String namespace, String municipalityId, String ownerId);

	Optional<NotificationEntity> findByNamespaceAndMunicipalityIdAndOwnerIdAndAcknowledgedAndErrandEntityIdAndType(final String namespace, final String municipalityId, final String ownerId, final boolean acknowledged, final String errandId,
		final String type);

	List<NotificationEntity> findByExpiresBefore(final OffsetDateTime expires);
}

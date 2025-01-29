package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
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

	boolean existsByNamespaceAndMunicipalityIdAndOwnerIdAndErrandEntityAndDescription(
		final String namespace,
		final String municipalityId,
		final String ownerId,
		final ErrandEntity errandEntity,
		final String description);

	Optional<NotificationEntity> findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(
		final String id,
		final String namespace,
		final String municipalityId,
		final String errandId);

	List<NotificationEntity> findAllByNamespaceAndMunicipalityIdAndErrandEntityId(
		final String namespace,
		final String municipalityId,
		final String errandId,
		final Sort sort);

	boolean existsByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(
		final String id,
		final String namespace,
		final String municipalityId,
		final String errandId);

	List<NotificationEntity> findAllByNamespaceAndMunicipalityIdAndOwnerId(
		final String namespace,
		final String municipalityId,
		final String ownerId);

	List<NotificationEntity> findByExpiresBefore(final OffsetDateTime expires);
}

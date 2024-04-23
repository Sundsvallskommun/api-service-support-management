package se.sundsvall.supportmanagement.integration.db;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@CircuitBreaker(name = "notificationRepository")
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	Optional<NotificationEntity> findByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	List<NotificationEntity> findAllByNamespaceAndMunicipalityIdAndOwnerId(String namespace, String municipalityId, String ownerId);

}

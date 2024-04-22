package se.sundsvall.supportmanagement.integration.db;

import org.springframework.data.jpa.repository.JpaRepository;

import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@CircuitBreaker(name = "notificationRepository")
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

}

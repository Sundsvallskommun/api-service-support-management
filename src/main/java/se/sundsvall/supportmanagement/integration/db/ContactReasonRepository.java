package se.sundsvall.supportmanagement.integration.db;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Repository
@CircuitBreaker(name = "contactReasonRepository")
public interface ContactReasonRepository extends JpaRepository<ContactReasonEntity, Long> {

	Optional<ContactReasonEntity> findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(final String reason, final String namespace, final String municipalityId);

	ContactReasonEntity getByReasonIgnoreCaseAndNamespaceAndMunicipalityId(final String reason, final String namespace, final String municipalityId);

	List<ContactReasonEntity> findAllByNamespaceAndMunicipalityId(final String namespace, final String municipalityId);

	Boolean existsByReasonIgnoreCaseAndNamespaceAndMunicipalityId(final String reason, final String namespace, final String municipalityId);

	void deleteByReasonIgnoreCaseAndNamespaceAndMunicipalityId(final String reason, final String namespace, final String municipalityId);
}

package se.sundsvall.supportmanagement.integration.db;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;

@CircuitBreaker(name = "contactReasonRepository")
public interface ContactReasonRepository extends JpaRepository<ContactReasonEntity, Long> {

	Optional<ContactReasonEntity> findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(final String reason, final String namespace, final String municipalityId);

	Optional<ContactReasonEntity> findByIdAndNamespaceAndMunicipalityId(final Long id, final String namespace, final String municipalityId);

	ContactReasonEntity getByIdAndNamespaceAndMunicipalityId(final Long id, final String namespace, final String municipalityId);

	List<ContactReasonEntity> findAllByNamespaceAndMunicipalityId(final String namespace, final String municipalityId);

	boolean existsByIdAndNamespaceAndMunicipalityId(final Long id, final String namespace, final String municipalityId);

	void deleteByIdAndNamespaceAndMunicipalityId(final Long id, final String namespace, final String municipalityId);
}

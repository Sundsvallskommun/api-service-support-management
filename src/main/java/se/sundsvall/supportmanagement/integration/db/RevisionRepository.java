package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.RevisionEntity;

@CircuitBreaker(name = "revisionRepository")
public interface RevisionRepository extends JpaRepository<RevisionEntity, String> {

	/**
	 * Find the revision matching provided entityId and version.
	 *
	 * @param  entityId the id for the errand entity to fetch revision for.
	 * @param  version  the version to fetch.
	 * @return          an optional entity that matches the provided parameters.
	 */
	Optional<RevisionEntity> findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(String namespace, String municipalityId, String entityId, int version);

	/**
	 * Find the last revision by entityId.
	 *
	 * @param  entityId the id for the errand entity to find the last revision version for.
	 * @return          an optional entity that matches the provided parameters (i.e. last created revision for an entity).
	 */
	Optional<RevisionEntity> findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(String namespace, String municipalityId, String entityId);

	/**
	 * Find all revisions for an errand entity.
	 *
	 * @param  entityId the id for the errand entity to find all revision versions for.
	 * @return          a list of RevisionEntity objects.
	 */
	List<RevisionEntity> findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(String namespace, String municipalityId, String entityId);
}

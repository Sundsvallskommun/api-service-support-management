package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportmanagement.integration.db.model.IdProjection;
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

	/**
	 * Find the ids of every revision of an errand.
	 * <p>
	 * Only the ids are read, and that is the point of the method. A revision holds a full serialized snapshot of the
	 * errand, so a removal that reads whole revisions before removing any would hold every snapshot of the errand at
	 * once - which for a long lived errand is more than the heap has to spare.
	 *
	 * @param  namespace      namespace of the errand.
	 * @param  municipalityId id of the municipality of the errand.
	 * @param  entityId       id of the errand.
	 * @return                the ids of every revision of the errand.
	 */
	List<IdProjection> findIdsByNamespaceAndMunicipalityIdAndEntityId(String namespace, String municipalityId, String entityId);
}

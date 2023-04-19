package se.sundsvall.supportmanagement.integration.db;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.sundsvall.supportmanagement.integration.db.model.RevisionEntity;

@Transactional
@CircuitBreaker(name = "revisionRepository")
public interface RevisionRepository extends CrudRepository<RevisionEntity, String> {

	/**
	 * Find the revision matching provided entityId and version.
	 *
	 * @param entityId the id for the errand entity to fetch revision for.
	 * @param version  the version to fetch.
	 * @return an optional entity that matches the provided parameters.
	 */
	Optional<RevisionEntity> findByEntityIdAndVersion(String entityId, int version);

	/**
	 * Find the last revision by entityId.
	 *
	 * @param entityId the id for the errand entity to find the last revision version for.
	 * @return an optional entity that matches the provided parameters (i.e. last created revision for an entity).
	 */
	Optional<RevisionEntity> findFirstByEntityIdOrderByVersionDesc(String entityId);

	/**
	 * Find all RevisionEntity-objects with a version matching the provided list.
	 *
	 * @param entityId the id for the errand entity to fetch revision for.
	 * @param versions the list of versions to return.
	 * @return a list of RevisionEntity objects.
	 */
	List<RevisionEntity> findByEntityIdAndVersionIn(String entityId, Integer... versions);

	/**
	 * Find all revisions for an errand entity.
	 *
	 * @param entityId the id for the errand entity to find all revision versions for.
	 * @return a list of RevisionEntity objects.
	 */
	List<RevisionEntity> findByEntityId(String entityId);
}

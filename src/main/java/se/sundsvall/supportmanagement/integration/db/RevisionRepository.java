package se.sundsvall.supportmanagement.integration.db;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import se.sundsvall.supportmanagement.integration.db.model.RevisionEntity;

@Transactional
@CircuitBreaker(name = "revisionRepository")
public interface RevisionRepository extends CrudRepository<RevisionEntity, String> {
	/**
	 * Find the revision by entityId and entityType.
	 *
	 * @param entityId the entityId for the revision.
	 * @param version  the version for the revision.
	 * @return an optional entity that matches the provided parameters.
	 */
	Optional<RevisionEntity> findByEntityIdAndVersion(String entityId, int version);

	/**
	 * Find the last revision by entityId.
	 *
	 * @param entityId the entityId to find revisions for.
	 * @return an optional entity that matches the provided parameters (i.e. last created revision for an entity).
	 */
	Optional<RevisionEntity> findFirstByEntityIdOrderByVersionDesc(String entityId);
}

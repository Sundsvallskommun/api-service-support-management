package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * Removes rows a chunk at a time, flushing and emptying the persistence context after each chunk, which bounds the
 * heap a bulk removal holds to what a single chunk costs.
 * <p>
 * Everything in the persistence context of the transaction is detached by this, the entity a caller passed in
 * included. Whatever is needed of such an entity afterwards has to be read before the removal.
 */
@Component
public class ChunkedDeleter {

	/**
	 * How many rows are removed before the persistence context is emptied.
	 */
	private static final int CHUNK_SIZE = 50;

	private final EntityManager entityManager;

	public ChunkedDeleter(final EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * Removes the sent in ids a chunk at a time. An empty list leaves the persistence context alone.
	 *
	 * @param ids    ids of the rows to remove.
	 * @param delete how a chunk of ids is removed.
	 */
	public <T> void deleteInChunks(final List<T> ids, final Consumer<List<T>> delete) {
		for (var start = 0; start < ids.size(); start += CHUNK_SIZE) {
			delete.accept(ids.subList(start, Math.min(start + CHUNK_SIZE, ids.size())));

			entityManager.flush();
			entityManager.clear();
		}
	}
}

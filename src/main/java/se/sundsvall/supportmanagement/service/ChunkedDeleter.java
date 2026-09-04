package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * Removes rows a chunk at a time, emptying the persistence context between chunks.
 * <p>
 * What a bulk removal runs out of is heap rather than time. The rows hanging off an errand are where its bulk sits -
 * revision snapshots and message bodies are held as long text - and a removal that reads every one of them before
 * removing any keeps all of them, along with the copy Hibernate holds of each to compare against, until the
 * transaction commits. Chunking bounds that to what a single chunk costs.
 * <p>
 * Clearing is what makes the bound hold, since a chunk left managed is a chunk still in the heap. Flushing first is
 * what carries the removals to the database: clearing on its own discards them.
 * <p>
 * The persistence context belongs to the transaction rather than to a caller, so everything in it is detached by this,
 * the entity a caller passed in included. Whatever is needed of such an entity afterwards has to be read before the
 * removal, and the callers here say where they do it.
 */
@Component
public class ChunkedDeleter {

	/**
	 * How many rows are removed before the persistence context is emptied. What a chunk costs is set by the heaviest row
	 * in it, and the rows this is used for carry long text, so it is held well below what a batch of ordinary rows could
	 * be.
	 */
	private static final int CHUNK_SIZE = 50;

	private final EntityManager entityManager;

	public ChunkedDeleter(final EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * Removes the sent in ids a chunk at a time. An empty list leaves the persistence context alone, since there is
	 * nothing to remove and nothing to make room for.
	 *
	 * @param ids    ids of the rows to remove.
	 * @param delete how a chunk of ids is removed.
	 */
	public void deleteInChunks(final List<String> ids, final Consumer<List<String>> delete) {
		for (var start = 0; start < ids.size(); start += CHUNK_SIZE) {
			delete.accept(ids.subList(start, Math.min(start + CHUNK_SIZE, ids.size())));

			entityManager.flush();
			entityManager.clear();
		}
	}
}

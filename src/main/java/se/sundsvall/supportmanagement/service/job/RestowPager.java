package se.sundsvall.supportmanagement.service.job;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import org.slf4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

/**
 * Keyset-paged, optimistic-lock-retrying walk over a batch of {@link ErrandEntity} at a time - shared shape behind
 * {@link LabelMoveWorker}'s and {@link LabelMergeWorker}'s restow loops, which read a page, hand it to a batch
 * persist that can lose an optimistic-lock race against a concurrent edit, retry a fixed number of times against a
 * fresh read, and report cumulative progress once a page actually lands. Identical between the two workers except
 * how a page is fetched, how it is persisted, and what a retry is logged as - each supplied by the caller.
 */
final class RestowPager {

	private final Logger log;
	private final int batchSize;
	private final int maxAttempts;

	RestowPager(final Logger log, final int batchSize, final int maxAttempts) {
		this.log = log;
		this.batchSize = batchSize;
		this.maxAttempts = maxAttempts;
	}

	/**
	 * Walks every page {@code fetcher} returns starting from the beginning, handing each to {@code persister}, until a
	 * page comes back shorter than the configured batch size - necessarily the last one, since keyset paging has no
	 * separate "has next" signal to ask for.
	 *
	 * @param  fetcher          reads one page of errands starting after a given last-seen id.
	 * @param  persister        persists one page - may throw {@link ObjectOptimisticLockingFailureException} on a
	 *                          concurrent-edit conflict, retried against a fresh read up to the configured attempt
	 *                          limit.
	 * @param  retryMessage     the WARN message to log for a given attempt number, once a retry is about to happen.
	 * @param  progressReporter cumulative count of errands restowed so far.
	 * @return                  number of errands restowed.
	 */
	int restow(final PageFetcher fetcher, final PagePersister persister, final IntFunction<String> retryMessage, final IntConsumer progressReporter) {
		var lastSeenId = "";
		var processed = 0;
		var page = fetchAndPersistPage(fetcher, persister, retryMessage, lastSeenId);

		while (!page.isEmpty()) {
			processed += page.size();
			progressReporter.accept(processed);
			lastSeenId = page.get(page.size() - 1).getId();

			// A page shorter than requested is necessarily the last one - skip the round-trip that would only confirm it.
			page = page.size() < batchSize ? List.of() : fetchAndPersistPage(fetcher, persister, retryMessage, lastSeenId);
		}

		return processed;
	}

	/**
	 * Reads one page and hands it to {@code persister}, retrying against a fresh read when a concurrent edit - a user
	 * PATCHing one of these errands between the read and the merge, which {@code ErrandEntity}'s {@code @Version} turns
	 * into a lock conflict rather than a silently lost update - loses the optimistic-lock race. A stale,
	 * already-detached page would just fail the same way again, so each attempt re-reads rather than retrying the same
	 * instances; an errand a concurrent edit has since unlabelled naturally drops out of the requery instead of being
	 * retried at all.
	 */
	private List<ErrandEntity> fetchAndPersistPage(final PageFetcher fetcher, final PagePersister persister, final IntFunction<String> retryMessage, final String lastSeenId) {
		final var pageable = PageRequest.ofSize(batchSize);
		var attempt = 0;

		while (true) {
			attempt++;
			final var page = fetcher.fetch(lastSeenId, pageable);
			if (page.isEmpty()) {
				return page;
			}

			try {
				persister.persist(page);
				return page;
			} catch (final ObjectOptimisticLockingFailureException e) {
				if (attempt == maxAttempts) {
					throw e;
				}
				log.warn(retryMessage.apply(attempt));
			}
		}
	}

	@FunctionalInterface
	interface PageFetcher {
		List<ErrandEntity> fetch(String lastSeenId, Pageable pageable);
	}

	@FunctionalInterface
	interface PagePersister {
		void persist(List<ErrandEntity> page);
	}
}

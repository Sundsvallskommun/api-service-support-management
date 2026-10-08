package se.sundsvall.supportmanagement.service;

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
	 * @param  progressReporter cumulative count of errands walked so far, those that kept their labels included.
	 * @return                  how many errands were restowed, and how many kept the labels they had.
	 */
	Outcome restow(final PageFetcher fetcher, final PagePersister persister, final IntFunction<String> retryMessage, final IntConsumer progressReporter) {
		var lastSeenId = "";
		var processed = 0;
		var unchanged = 0;
		var page = fetchAndPersistPage(fetcher, persister, retryMessage, lastSeenId);

		while (!page.errands().isEmpty()) {
			processed += page.errands().size();
			unchanged += page.unchanged();
			progressReporter.accept(processed);
			lastSeenId = page.errands().getLast().getId();

			// A page shorter than requested is necessarily the last one - skip the round-trip that would only confirm it.
			page = page.errands().size() < batchSize ? PersistedPage.EMPTY : fetchAndPersistPage(fetcher, persister, retryMessage, lastSeenId);
		}

		return new Outcome(processed - unchanged, unchanged);
	}

	/**
	 * Reads one page and hands it to {@code persister}, retrying against a fresh read when a concurrent edit - a user
	 * PATCHing one of these errands between the read and the merge, which {@code ErrandEntity}'s {@code @Version} turns
	 * into a lock conflict rather than a silently lost update - loses the optimistic-lock race. A stale,
	 * already-detached page would just fail the same way again, so each attempt re-reads rather than retrying the same
	 * instances; an errand a concurrent edit has since unlabelled naturally drops out of the requery instead of being
	 * retried at all.
	 * <p>
	 * A retried page is persisted from its first errand again, and only the database work of the failed attempt is rolled
	 * back. The update event a relabelled errand writes to the event log is sent when the errand is persisted, so the
	 * errands persisted before the conflict have the event written once more for every attempt.
	 */
	private PersistedPage fetchAndPersistPage(final PageFetcher fetcher, final PagePersister persister, final IntFunction<String> retryMessage, final String lastSeenId) {
		final var pageable = PageRequest.ofSize(batchSize);
		var attempt = 0;

		while (true) {
			attempt++;
			final var page = fetcher.fetch(lastSeenId, pageable);
			if (page.isEmpty()) {
				return PersistedPage.EMPTY;
			}

			try {
				return new PersistedPage(page, persister.persist(page));
			} catch (final ObjectOptimisticLockingFailureException e) {
				if (attempt == maxAttempts) {
					throw e;
				}
				log.warn(retryMessage.apply(attempt));
			}
		}
	}

	/**
	 * What a walk did with the errands it reached.
	 *
	 * @param restowed  the errands given their rebuilt labels.
	 * @param unchanged the errands that kept the labels they had, refused by a guard or left as they are for having no
	 *                  access labels.
	 */
	record Outcome(int restowed, int unchanged) {

		static final Outcome NONE = new Outcome(0, 0);

		private static final String RESTOWED = "%d errand(s) restowed";
		private static final String RESTOWED_AND_UNCHANGED = "%d errand(s) restowed, %d kept their labels";

		/**
		 * @return every errand reached, restowed or not.
		 */
		int processed() {
			return restowed + unchanged;
		}

		/**
		 * @param  other the outcome of another walk.
		 * @return       the two outcomes added together.
		 */
		Outcome plus(final Outcome other) {
			return new Outcome(restowed + other.restowed, unchanged + other.unchanged);
		}

		/**
		 * The outcome in words, for the summary of a job and the message of an audit event. The errands that kept their
		 * labels are named only when there are any.
		 *
		 * @return the outcome in words.
		 */
		String describe() {
			return unchanged == 0 ? RESTOWED.formatted(restowed) : RESTOWED_AND_UNCHANGED.formatted(restowed, unchanged);
		}
	}

	/**
	 * A page as persisted, with how many of its errands kept their labels.
	 */
	private record PersistedPage(List<ErrandEntity> errands, int unchanged) {

		private static final PersistedPage EMPTY = new PersistedPage(List.of(), 0);
	}

	@FunctionalInterface
	interface PageFetcher {
		List<ErrandEntity> fetch(String lastSeenId, Pageable pageable);
	}

	@FunctionalInterface
	interface PagePersister {

		/**
		 * @param  page the errands to persist.
		 * @return      how many of them kept the labels they had.
		 */
		int persist(List<ErrandEntity> page);
	}
}

package se.sundsvall.supportmanagement.service.scheduler.searchreindex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.supportmanagement.service.search.index.ErrandReindexService;

/**
 * Writes the search index over from the database every night, so that what indexing missed does not stay missed.
 * <p>
 * Indexing follows a commit without holding the request up, which is what keeps an unreachable OpenSearch from failing
 * a write, and is also why the index can come to differ from the database without anything saying so. Nothing is
 * rebuilt when the service starts: a deploy leaves the index as it is, and only the mapping is brought up to date.
 */
@Service
public class SearchReindexScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(SearchReindexScheduler.class);

	private final ErrandReindexService reindexService;
	private final boolean enabled;

	public SearchReindexScheduler(final ErrandReindexService reindexService,
		@Value("${scheduler.search-reindex.enabled:false}") final boolean enabled) {
		this.reindexService = reindexService;
		this.enabled = enabled;
	}

	@Dept44Scheduled(
		cron = "${scheduler.search-reindex.cron}",
		name = "${scheduler.search-reindex.name}",
		lockAtMostFor = "${scheduler.search-reindex.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.search-reindex.maximum-execution-time}")
	void reindexErrands() {
		if (!enabled) {
			LOG.info("scheduler.search-reindex.enabled=false, skipping scheduled execution");
			return;
		}
		reindexService.reindexEveryNamespace();
	}
}

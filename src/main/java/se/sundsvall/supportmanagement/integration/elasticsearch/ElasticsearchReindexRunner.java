package se.sundsvall.supportmanagement.integration.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Reindexes all errands having json parameters into Elasticsearch at application startup. Opt-in via
 * {@code elasticsearch.reindex.on-startup=true} (requires {@code elasticsearch.enabled=true}).
 * <p>
 * The reindex runs on a background daemon thread. Spring Boot publishes the application ready event only once every
 * {@link ApplicationRunner} has returned, so doing this work inline would hold readiness down for the entire run and
 * make the platform kill the instance before it ever starts serving traffic.
 * <p>
 * Errands are read in batches of 500, each in its own short transaction, and a failing batch is logged and skipped —
 * the run continues.
 */
@Component
@ConditionalOnProperty(name = {
	"elasticsearch.enabled", "elasticsearch.reindex.on-startup"
}, havingValue = "true")
public class ElasticsearchReindexRunner implements ApplicationRunner {

	private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchReindexRunner.class);
	private static final int BATCH_SIZE = 500;
	private static final String THREAD_NAME = "elasticsearch-reindex";

	private final ElasticsearchReindexer elasticsearchReindexer;

	public ElasticsearchReindexRunner(final ElasticsearchReindexer elasticsearchReindexer) {
		this.elasticsearchReindexer = elasticsearchReindexer;
	}

	@Override
	public void run(final ApplicationArguments args) {
		Thread.ofPlatform().name(THREAD_NAME).daemon(true).start(this::reindex);
	}

	void reindex() {
		try {
			final var totalCount = elasticsearchReindexer.countErrandsToIndex();
			LOG.info("Starting Elasticsearch reindex of {} errands with json parameters", totalCount);

			var indexedCount = 0;
			var failedBatches = 0;
			var batch = elasticsearchReindexer.indexBatch("", BATCH_SIZE);

			while (!batch.isEmpty()) {
				indexedCount += batch.indexedCount();
				if (batch.failed()) {
					failedBatches++;
				}
				LOG.info("Elasticsearch reindex progress: {} of {} errands indexed", indexedCount, totalCount);
				batch = elasticsearchReindexer.indexBatch(batch.lastId(), BATCH_SIZE);
			}

			LOG.info("Elasticsearch reindex finished: {} of {} errands indexed, {} failed batches", indexedCount, totalCount, failedBatches);
		} catch (final Exception e) {
			LOG.error("Elasticsearch reindex aborted", e);
		}
	}
}

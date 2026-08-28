package se.sundsvall.supportmanagement.integration.elasticsearch;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;

/**
 * Reindexes all errands having json parameters into Elasticsearch at application startup. Opt-in via
 * {@code elasticsearch.reindex.on-startup=true} (requires {@code elasticsearch.enabled=true}). Batches of 500 with the
 * Hibernate first-level cache cleared between batches to keep memory flat on large datasets. A failing batch is logged
 * and skipped — the run continues.
 */
@Component
@ConditionalOnProperty(name = {
	"elasticsearch.enabled", "elasticsearch.reindex.on-startup"
}, havingValue = "true")
public class ElasticsearchReindexRunner implements ApplicationRunner {

	private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchReindexRunner.class);
	private static final int BATCH_SIZE = 500;

	private final ErrandsRepository errandsRepository;
	private final ElasticsearchIndexService elasticsearchIndexService;
	private final EntityManager entityManager;

	public ElasticsearchReindexRunner(final ErrandsRepository errandsRepository, final ElasticsearchIndexService elasticsearchIndexService, final EntityManager entityManager) {
		this.errandsRepository = errandsRepository;
		this.elasticsearchIndexService = elasticsearchIndexService;
		this.entityManager = entityManager;
	}

	@Override
	@Transactional(readOnly = true)
	public void run(final ApplicationArguments args) {
		LOG.info("Starting Elasticsearch reindex of errands with json parameters");

		var pageRequest = PageRequest.of(0, BATCH_SIZE, Sort.by("id"));
		var indexedCount = 0;
		var failedBatches = 0;

		var page = errandsRepository.findAllByJsonParametersIsNotEmpty(pageRequest);
		final var totalCount = page.getTotalElements();

		while (!page.isEmpty()) {
			try {
				elasticsearchIndexService.indexAll(page.getContent());
				indexedCount += page.getNumberOfElements();
			} catch (final Exception e) {
				failedBatches++;
				LOG.warn("Failed to index batch {} during reindex: {}", pageRequest.getPageNumber(), e.getMessage());
			}
			entityManager.clear();

			if (!page.hasNext()) {
				break;
			}
			pageRequest = pageRequest.next();
			page = errandsRepository.findAllByJsonParametersIsNotEmpty(pageRequest);
		}

		LOG.info("Elasticsearch reindex finished: {} of {} errands indexed, {} failed batches", indexedCount, totalCount, failedBatches);
	}
}

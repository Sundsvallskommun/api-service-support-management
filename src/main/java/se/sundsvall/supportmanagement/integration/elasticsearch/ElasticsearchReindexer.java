package se.sundsvall.supportmanagement.integration.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;

import static java.util.Objects.isNull;

/**
 * Reads errands with json parameters one batch at a time on behalf of {@link ElasticsearchReindexRunner}. Each batch
 * runs in its own short read-only transaction, which keeps the lazily loaded json parameters reachable during mapping
 * without holding a single transaction open for the entire reindex.
 */
@Component
@ConditionalOnProperty(name = {
	"elasticsearch.enabled", "elasticsearch.reindex.on-startup"
}, havingValue = "true")
public class ElasticsearchReindexer {

	private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchReindexer.class);

	private final ErrandsRepository errandsRepository;
	private final ElasticsearchIndexService elasticsearchIndexService;

	public ElasticsearchReindexer(final ErrandsRepository errandsRepository, final ElasticsearchIndexService elasticsearchIndexService) {
		this.errandsRepository = errandsRepository;
		this.elasticsearchIndexService = elasticsearchIndexService;
	}

	public long countErrandsToIndex() {
		return errandsRepository.countByJsonParametersIsNotEmpty();
	}

	/**
	 * Indexes the batch of errands following the sent in id. A failing batch is logged rather than propagated, but still
	 * reports the id it reached so the caller can move past it instead of retrying it forever.
	 *
	 * @param  afterId   id to continue after, empty string to start from the beginning
	 * @param  batchSize maximum number of errands to read
	 * @return           outcome of the batch, empty when there is nothing left to index
	 */
	@Transactional(readOnly = true)
	public BatchResult indexBatch(final String afterId, final int batchSize) {
		final var errands = errandsRepository.findByJsonParametersIsNotEmptyAndIdGreaterThanOrderByIdAsc(afterId, Limit.of(batchSize));

		if (errands.isEmpty()) {
			return BatchResult.empty();
		}

		final var lastId = errands.getLast().getId();

		try {
			elasticsearchIndexService.indexAll(errands);
			return new BatchResult(lastId, errands.size(), false);
		} catch (final Exception e) {
			LOG.warn("Failed to index batch of {} errands ending at id {}: {}", errands.size(), lastId, e.getMessage());
			return new BatchResult(lastId, 0, true);
		}
	}

	/**
	 * Outcome of a single reindex batch.
	 *
	 * @param lastId       id of the last errand read, null when no errands remained
	 * @param indexedCount number of errands successfully indexed
	 * @param failed       whether indexing of the batch failed
	 */
	public record BatchResult(String lastId, int indexedCount, boolean failed) {

		static BatchResult empty() {
			return new BatchResult(null, 0, false);
		}

		boolean isEmpty() {
			return isNull(lastId);
		}
	}
}

package se.sundsvall.supportmanagement.integration.elasticsearch;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.mapper.JsonParameterDocumentMapper;

import static se.sundsvall.supportmanagement.service.mapper.JsonParameterDocumentMapper.hasJsonParameters;
import static se.sundsvall.supportmanagement.service.mapper.JsonParameterDocumentMapper.toJsonParameterDocument;

/**
 * Maintains the Elasticsearch json parameter index. MariaDB is the source of truth — single document operations catch
 * and log failures without propagating them, so an Elasticsearch outage never affects database operations.
 */
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ElasticsearchIndexService {

	private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchIndexService.class);

	private final JsonParameterDocumentRepository repository;

	public ElasticsearchIndexService(final JsonParameterDocumentRepository repository) {
		this.repository = repository;
	}

	/**
	 * Indexes the errand's json parameters, or removes the document when the errand no longer has any json parameters.
	 * Failures are logged and never propagated.
	 *
	 * @param errandEntity the errand to index
	 */
	public void index(final ErrandEntity errandEntity) {
		try {
			if (hasJsonParameters(errandEntity)) {
				repository.save(toJsonParameterDocument(errandEntity));
			} else {
				repository.deleteById(errandEntity.getId());
			}
		} catch (final Exception e) {
			LOG.warn("Failed to index errand {} in Elasticsearch: {}", errandEntity.getId(), e.getMessage());
		}
	}

	/**
	 * Removes the errand's document from the index. Failures are logged and never propagated.
	 *
	 * @param errandId id of the errand to remove
	 */
	public void delete(final String errandId) {
		try {
			repository.deleteById(errandId);
		} catch (final Exception e) {
			LOG.warn("Failed to delete errand {} from Elasticsearch: {}", errandId, e.getMessage());
		}
	}

	/**
	 * Indexes all sent in errands that have json parameters. Used by the reindex runner — failures propagate so the
	 * caller can decide whether to continue.
	 *
	 * @param errandEntities errands to index
	 */
	public void indexAll(final Collection<ErrandEntity> errandEntities) {
		final var documents = errandEntities.stream()
			.filter(JsonParameterDocumentMapper::hasJsonParameters)
			.map(JsonParameterDocumentMapper::toJsonParameterDocument)
			.toList();

		if (!documents.isEmpty()) {
			repository.saveAll(documents);
		}
	}
}

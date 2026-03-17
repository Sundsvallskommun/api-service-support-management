package se.sundsvall.supportmanagement.integration.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;

import static java.util.Optional.ofNullable;

public class JsonParameterSearchService {

	private static final Logger LOG = LoggerFactory.getLogger(JsonParameterSearchService.class);

	private final ElasticsearchOperations elasticsearchOperations;

	public JsonParameterSearchService(final ElasticsearchOperations elasticsearchOperations) {
		this.elasticsearchOperations = elasticsearchOperations;
	}

	/**
	 * Searches for errand IDs where a JSON parameter matches the given key and value criteria.
	 *
	 * @param  namespace      the namespace to search within
	 * @param  municipalityId the municipality ID to search within
	 * @param  parameterKey   the JSON parameter key to match
	 * @param  valueFilters   map of JSON field paths to expected values (e.g., {"address.city": "Sundsvall"})
	 * @return                list of matching errand IDs
	 */
	public List<String> findErrandIdsByJsonParameterValues(final String namespace, final String municipalityId,
		final String parameterKey, final Map<String, Object> valueFilters) {

		final var boolQueryBuilder = QueryBuilders.bool();

		// Always filter on namespace and municipalityId
		boolQueryBuilder.filter(f -> f.term(t -> t.field("namespace").value(namespace)));
		boolQueryBuilder.filter(f -> f.term(t -> t.field("municipalityId").value(municipalityId)));

		// Filter on parameterKey if provided
		ofNullable(parameterKey).ifPresent(key -> boolQueryBuilder.filter(f -> f.term(t -> t.field("parameterKey").value(key))));

		// Add value field filters
		ofNullable(valueFilters).ifPresent(filters -> filters.forEach((path, expectedValue) -> boolQueryBuilder.must(m -> m.match(mt -> mt.field("value." + path).query(expectedValue.toString())))));

		final var query = NativeQuery.builder()
			.withQuery(q -> q.bool(boolQueryBuilder.build()))
			.build();

		LOG.debug("Executing Elasticsearch query for JSON parameters: namespace={}, municipalityId={}, key={}, filters={}", namespace, municipalityId, parameterKey, valueFilters);

		final var hits = elasticsearchOperations.search(query, JsonParameterDocument.class);
		return hits.getSearchHits().stream()
			.map(SearchHit::getContent)
			.map(JsonParameterDocument::getErrandId)
			.distinct()
			.toList();
	}
}

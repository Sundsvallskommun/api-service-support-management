package se.sundsvall.supportmanagement.integration.elasticsearch;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;
import se.sundsvall.supportmanagement.integration.elasticsearch.model.JsonParameterDocument;

/**
 * Queries the Elasticsearch json parameter index. Queries are always scoped by namespace and municipalityId. This
 * service throws on Elasticsearch failure — the caller decides how to degrade.
 */
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ElasticsearchSearchService {

	private static final String DEFAULT_FIELD = "jsonParameters.*";
	private static final String NAMESPACE_FIELD = "namespace";
	private static final String MUNICIPALITY_ID_FIELD = "municipalityId";
	private static final int MAX_RESULTS = 10_000;

	private final ElasticsearchOperations elasticsearchOperations;

	public ElasticsearchSearchService(final ElasticsearchOperations elasticsearchOperations) {
		this.elasticsearchOperations = elasticsearchOperations;
	}

	/**
	 * Searches the json parameter index with an Elasticsearch query_string (Lucene syntax) query and returns the ids of
	 * matching errands, capped at 10 000.
	 *
	 * @param  namespace      namespace to scope the query by
	 * @param  municipalityId municipality id to scope the query by
	 * @param  queryString    query in Elasticsearch query_string syntax
	 * @return                ids of matching errands
	 */
	public List<String> query(final String namespace, final String municipalityId, final String queryString) {
		final var nativeQuery = NativeQuery.builder()
			.withQuery(query -> query.bool(bool -> bool
				.must(must -> must.queryString(qs -> qs
					.query(queryString)
					.defaultField(DEFAULT_FIELD)
					.lenient(true)))
				.filter(filter -> filter.term(term -> term.field(NAMESPACE_FIELD).value(namespace)))
				.filter(filter -> filter.term(term -> term.field(MUNICIPALITY_ID_FIELD).value(municipalityId)))))
			.withMaxResults(MAX_RESULTS)
			.build();

		return elasticsearchOperations.search(nativeQuery, JsonParameterDocument.class)
			.stream()
			.map(SearchHit::getId)
			.toList();
	}
}

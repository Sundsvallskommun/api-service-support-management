package se.sundsvall.supportmanagement.service.search;

import org.elasticsearch.client.Request;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reports whether the OpenSearch cluster behind the search index answers, and how it says it is doing.
 * <p>
 * Part of the overall health only, and deliberately not of the liveness or readiness of the service: a lost OpenSearch
 * makes search unavailable, which is worth knowing, but restarting the pod or taking it out of rotation would not bring
 * it back and would take everything else down with it.
 */
@Component("openSearch")
@ConditionalOnProperty(name = "spring.jpa.properties.hibernate.search.enabled", havingValue = "true")
public class OpenSearchHealthIndicator implements HealthIndicator {

	static final String CLUSTER_STATUS = "clusterStatus";
	static final String CLUSTER_NAME = "clusterName";

	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	private final OpenSearchClient openSearch;

	public OpenSearchHealthIndicator(final OpenSearchClient openSearch) {
		this.openSearch = openSearch;
	}

	@Override
	public Health health() {
		try {
			final var response = openSearch.restClient().performRequest(new Request("GET", "/_cluster/health"));
			final var body = MAPPER.readTree(response.getEntity().getContent());
			final var status = body.path("status").asString();

			return ("red".equals(status) ? Health.down() : Health.up())
				.withDetail(CLUSTER_NAME, body.path("cluster_name").asString())
				.withDetail(CLUSTER_STATUS, status)
				.build();
		} catch (final Exception e) {
			return Health.down(e).build();
		}
	}
}

package se.sundsvall.supportmanagement.service.search.index;

import org.elasticsearch.client.Request;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reports whether the OpenSearch cluster behind the search index answers, and how it says it is doing.
 * <p>
 * Part of the overall health only, and deliberately not of the liveness or readiness of the service: a lost OpenSearch
 * makes search unavailable, which is worth knowing, but restarting the pod or taking it out of rotation would not bring
 * it back and would take everything else down with it. The same holds for search given up on by
 * {@link ErrandIndexModel}: the cluster may be perfectly well and searching it still refused, and this is where that
 * shows.
 * <p>
 * An index that wants a full reindex, or whose mapping is close to the number of fields it may hold, is reported as
 * restricted: search works, but the index has to be seen to before it gets worse. Neither is asked of the cluster
 * here, see {@link SearchIndexHealth}.
 */
@Component("openSearch")
@ConditionalOnProperty(name = "spring.jpa.properties.hibernate.search.enabled", havingValue = "true")
public class OpenSearchHealthIndicator implements HealthIndicator {

	static final String CLUSTER_STATUS = "clusterStatus";
	static final String CLUSTER_NAME = "clusterName";
	static final String SEARCH = "search";
	static final String FULL_REINDEX_WANTED = "fullReindexWanted";
	static final String FIELDS = "fields";
	static final String FIELDS_LIMIT = "fieldsLimit";
	static final String FIELDS_NEAR_LIMIT = "fieldsNearLimit";
	static final String NEAR_LIMIT = "The search index holds %d of the %d fields it may hold. A document bringing a field beyond the limit is refused";
	static final int FIELDS_WARNING_PERCENT = 80;
	static final Status RESTRICTED = new Status("RESTRICTED");

	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	private final OpenSearchClient openSearch;
	private final SearchAvailability availability;
	private final SearchIndexHealth indexHealth;

	public OpenSearchHealthIndicator(final OpenSearchClient openSearch, final SearchAvailability availability, final SearchIndexHealth indexHealth) {
		this.openSearch = openSearch;
		this.availability = availability;
		this.indexHealth = indexHealth;
	}

	@Override
	public Health health() {
		final var unusable = availability.unusable();
		if (unusable.isPresent()) {
			return Health.down().withDetail(SEARCH, unusable.get()).build();
		}

		try {
			final var response = openSearch.restClient().performRequest(new Request("GET", "/_cluster/health"));
			final var body = MAPPER.readTree(response.getEntity().getContent());
			final var status = body.path("status").asString();

			if ("red".equals(status)) {
				return Health.down()
					.withDetail(CLUSTER_NAME, body.path("cluster_name").asString())
					.withDetail(CLUSTER_STATUS, status)
					.build();
			}

			final var health = Health.up()
				.withDetail(CLUSTER_NAME, body.path("cluster_name").asString())
				.withDetail(CLUSTER_STATUS, status);
			return withIndexState(health).build();
		} catch (final Exception e) {
			return Health.down(e).build();
		}
	}

	private Health.Builder withIndexState(final Health.Builder health) {
		indexHealth.fieldUsage().ifPresent(usage -> {
			health.withDetail(FIELDS, usage.count()).withDetail(FIELDS_LIMIT, usage.limit());
			if (usage.percent() >= FIELDS_WARNING_PERCENT) {
				health.status(RESTRICTED).withDetail(FIELDS_NEAR_LIMIT, NEAR_LIMIT.formatted(usage.count(), usage.limit()));
			}
		});
		indexHealth.fullReindexWanted().ifPresent(reason -> health.status(RESTRICTED).withDetail(FULL_REINDEX_WANTED, reason));
		return health;
	}
}

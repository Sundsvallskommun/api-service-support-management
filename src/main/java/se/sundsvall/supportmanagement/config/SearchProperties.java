package se.sundsvall.supportmanagement.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the OpenSearch backed errand search that are not Hibernate Search's own (those live under
 * {@code spring.jpa.properties.hibernate.search}).
 *
 * @param maxResultWindow highest offset plus page size a search may ask for. Mirrors the
 *                        {@code index.max_result_window}
 *                        setting of the index, beyond which OpenSearch refuses to page, so that the client is told up
 *                        front instead of by a failed query.
 * @param timeout         how long a single search may run before the cluster gives up on it. A query string may ask
 *                        for work the index cannot do cheaply - a wildcard open at both ends, a regular expression, a
 *                        fuzzy term - and one client asking for it must not take the cluster away from everyone else.
 * @param reindex         settings for the mass indexer.
 */
@ConfigurationProperties(prefix = "search")
public record SearchProperties(

	@DefaultValue("10000") int maxResultWindow,

	@DefaultValue("PT10S") Duration timeout,

	@DefaultValue Reindex reindex) {

	/**
	 * @param lockAtMostFor how long the lock guarding a reindex is held at most. A reindex that has not released the lock
	 *                      within this time is taken to have died with its pod, and a new one may start.
	 */
	public record Reindex(@DefaultValue("PT6H") Duration lockAtMostFor) {
	}
}

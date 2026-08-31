package se.sundsvall.supportmanagement.integration.elasticsearch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Activates the OpenSearch-backed repository support when the feature flag is on. The spring-data-opensearch starter
 * ships no imperative repositories autoconfiguration, so this configuration is the only thing that registers the
 * repository — without it no OpenSearch bootstrapping (index creation, mapping writes) ever happens.
 */
@Configuration
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
@EnableElasticsearchRepositories(basePackages = "se.sundsvall.supportmanagement.integration.elasticsearch")
public class ElasticsearchConfiguration {
}

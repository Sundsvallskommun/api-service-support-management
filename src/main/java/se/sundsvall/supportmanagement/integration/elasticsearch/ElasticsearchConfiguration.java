package se.sundsvall.supportmanagement.integration.elasticsearch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Activates the Elasticsearch repository support when the feature flag is on. The repositories autoconfiguration is
 * disabled in application.yml (spring.data.elasticsearch.repositories.enabled=false), so without this configuration no
 * Elasticsearch bootstrapping (index creation, mapping writes) ever happens.
 */
@Configuration
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
@EnableElasticsearchRepositories(basePackages = "se.sundsvall.supportmanagement.integration.elasticsearch")
public class ElasticsearchConfiguration {
}

package se.sundsvall.supportmanagement.integration.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;

@Configuration
@ConditionalOnProperty(name = "integration.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
@EnableElasticsearchRepositories(basePackageClasses = JsonParameterElasticRepository.class)
public class ElasticsearchConfiguration {

	@Bean
	JsonParameterSearchService jsonParameterSearchService(final ElasticsearchOperations elasticsearchOperations) {
		return new JsonParameterSearchService(elasticsearchOperations);
	}

	@Bean
	JsonParameterSyncEventListener jsonParameterSyncEventListener(final ErrandsRepository errandsRepository,
		final JsonParameterElasticRepository elasticRepository,
		final ObjectMapper objectMapper) {
		return new JsonParameterSyncEventListener(errandsRepository, elasticRepository, objectMapper);
	}
}

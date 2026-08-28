package se.sundsvall.supportmanagement.integration.elasticsearch;

import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchConfigurationTest {

	@Test
	void verifyRepositoryActivation() {
		final var annotation = ElasticsearchConfiguration.class.getAnnotation(EnableElasticsearchRepositories.class);

		assertThat(new ElasticsearchConfiguration()).isNotNull();
		assertThat(annotation).isNotNull();
		assertThat(annotation.basePackages()).containsExactly("se.sundsvall.supportmanagement.integration.elasticsearch");
	}
}

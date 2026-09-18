package se.sundsvall.supportmanagement.integration.db.search;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchAnalysisConfigurerTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ElasticsearchAnalysisConfigurationContext contextMock;

	@Test
	void configure() {
		new SearchAnalysisConfigurer().configure(contextMock);

		verify(contextMock.tokenFilter("swedish_light_stemmer").type("stemmer")).param("language", "light_swedish");
		verify(contextMock.analyzer(SearchAnalysisConfigurer.TEXT).custom().tokenizer("standard")).tokenFilters("lowercase", "swedish_light_stemmer");
		verify(contextMock.normalizer(SearchAnalysisConfigurer.LOWERCASE).custom()).tokenFilters("lowercase");
	}
}

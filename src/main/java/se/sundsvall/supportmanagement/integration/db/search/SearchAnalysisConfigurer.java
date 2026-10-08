package se.sundsvall.supportmanagement.integration.db.search;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

/**
 * The analyzers the errand index is built with. Referenced from the entity mappings by the constants, and handed to
 * Hibernate Search through {@code hibernate.search.backend.analysis.configurer}.
 */
public class SearchAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {

	/**
	 * For free text: split into words, lowercased and stemmed for Swedish, so that a search for "läckor" finds "läcka"
	 * and case never matters.
	 */
	public static final String TEXT = "text_sv";

	/** For codes, names of things and sort keys: the whole value as one token, lowercased so that matching ignores case. */
	public static final String LOWERCASE = "lowercase_normalizer";

	private static final String SWEDISH_STEMMER = "swedish_light_stemmer";

	@Override
	public void configure(final ElasticsearchAnalysisConfigurationContext context) {
		context.tokenFilter(SWEDISH_STEMMER)
			.type("stemmer")
			.param("language", "light_swedish");

		context.analyzer(TEXT).custom()
			.tokenizer("standard")
			.tokenFilters("lowercase", SWEDISH_STEMMER);

		context.normalizer(LOWERCASE).custom()
			.tokenFilters("lowercase");
	}
}

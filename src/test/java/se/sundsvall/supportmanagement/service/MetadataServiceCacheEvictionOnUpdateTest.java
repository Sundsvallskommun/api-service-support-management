package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.Label;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ContextConfiguration
@ExtendWith(SpringExtension.class)
class MetadataServiceCacheEvictionOnUpdateTest {
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String NAMESPACE = "namespace";

	private MetadataService mock;

	@Autowired
	private MetadataService metadataService;

	// Provides a mock implementation for the service and a cache manager
	@EnableCaching
	@Configuration
	public static class CachingTestConfig {

		@Bean
		public MetadataService metadataServiceMockImplementation() {
			return mock(MetadataService.class);
		}

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("metadataCache", "metadataLabelsByPatternCache");
		}
	}

	@BeforeEach
	void setUp() {
		// AccessMapperService is a proxy around our mock. So, in order to use Mockito validations, we retrieve the actual mock
		// via AopTestUtils.getTargetObject
		mock = AopTestUtils.getTargetObject(metadataService);

		// reset(mock) is called between each test because CachingTestConfig only loads once
		reset(mock);
	}

	@DirtiesContext
	@ParameterizedTest
	@ValueSource(strings = {
		"category", "label"
	})
	void testCacheEvictionForAll(String type) {
		// First call should trigger logic in wrapped service class
		metadataService.findAll(NAMESPACE, MUNICIPALITY_ID);

		switch (type) {
			case "category" -> metadataService.updateCategory(NAMESPACE, MUNICIPALITY_ID, null, null);
			case "label" -> metadataService.updateLabels(NAMESPACE, MUNICIPALITY_ID, null);
			default -> throw new IllegalArgumentException("Unexpected value: " + type);
		}

		// Second call should trigger logic service class again and not use cache
		metadataService.findAll(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findAll(NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void testCacheEvictionForCategory() {
		final var categoryName = "categoryName";
		final var category = Category.create();

		// First call should trigger logic in wrapped service class
		metadataService.findCategories(NAMESPACE, MUNICIPALITY_ID);

		// Call to update object should remove key from cache
		metadataService.updateCategory(NAMESPACE, MUNICIPALITY_ID, categoryName, category);

		// Second call should trigger logic service class again and not use cache
		metadataService.findCategories(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findCategories(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).updateCategory(NAMESPACE, MUNICIPALITY_ID, categoryName, category);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForType() {
		final var categoryName = "categoryName";
		final var category = Category.create();

		// First call should trigger logic in wrapped service class
		metadataService.findTypes(NAMESPACE, MUNICIPALITY_ID, categoryName);

		// Call to update object should remove key from cache
		metadataService.updateCategory(NAMESPACE, MUNICIPALITY_ID, categoryName, category);

		// Second call should trigger logic service class again and not use cache
		metadataService.findTypes(NAMESPACE, MUNICIPALITY_ID, categoryName);

		verify(mock, times(2)).findTypes(NAMESPACE, MUNICIPALITY_ID, categoryName);
		verify(mock).updateCategory(NAMESPACE, MUNICIPALITY_ID, categoryName, category);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForLabel() {
		final var labels = List.of(Label.create());

		// First call should trigger logic in wrapped service class
		metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID);

		// Call to update object should remove key from cache
		metadataService.updateLabels(NAMESPACE, MUNICIPALITY_ID, labels);

		// Second call should trigger logic service class again and not use cache
		metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findLabels(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).updateLabels(NAMESPACE, MUNICIPALITY_ID, labels);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForPatternToLabel() {
		final var labels = List.of(Label.create());

		// First call should trigger logic in wrapped service class
		metadataService.patternToLabels(NAMESPACE, MUNICIPALITY_ID, null);

		// Call to update object should remove key from cache
		metadataService.updateLabels(NAMESPACE, MUNICIPALITY_ID, labels);

		// Second call should trigger logic service class again and not use cache
		metadataService.patternToLabels(NAMESPACE, MUNICIPALITY_ID, null);

		verify(mock, times(2)).patternToLabels(NAMESPACE, MUNICIPALITY_ID, null);
		verify(mock).updateLabels(NAMESPACE, MUNICIPALITY_ID, labels);
		verifyNoMoreInteractions(mock);
	}
}

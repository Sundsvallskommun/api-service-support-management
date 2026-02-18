package se.sundsvall.supportmanagement.service;

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ContextConfiguration
@ExtendWith(SpringExtension.class)
class MetadataServiceCacheEvictionOnDeleteTest {
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
		"category", "contactReason", "externalIdType", "label", "role", "status"
	})
	void testCacheEvictionForAll(String type) {
		// First call should trigger logic in wrapped service class
		metadataService.findAll(NAMESPACE, MUNICIPALITY_ID);

		switch (type) {
			case "category" -> metadataService.deleteCategory(NAMESPACE, MUNICIPALITY_ID, null);
			case "contactReason" -> metadataService.deleteContactReason(1L, NAMESPACE, MUNICIPALITY_ID);
			case "externalIdType" -> metadataService.deleteExternalIdType(NAMESPACE, MUNICIPALITY_ID, null);
			case "label" -> metadataService.deleteLabels(NAMESPACE, MUNICIPALITY_ID);
			case "role" -> metadataService.deleteRole(NAMESPACE, MUNICIPALITY_ID, null);
			case "status" -> metadataService.deleteStatus(NAMESPACE, MUNICIPALITY_ID, null);
			default -> throw new IllegalArgumentException("Unexpected value: " + type);
		}

		// Second call should trigger logic service class again and not use cache
		metadataService.findAll(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findAll(NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void testCacheEvictionForCategory() {
		// First call should trigger logic in wrapped service class
		metadataService.findCategories(NAMESPACE, MUNICIPALITY_ID);

		// Call to delete object should remove key from cache
		metadataService.deleteCategory(NAMESPACE, MUNICIPALITY_ID, null);

		// Second call should trigger logic service class again and not use cache
		metadataService.findCategories(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findCategories(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).deleteCategory(NAMESPACE, MUNICIPALITY_ID, null);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForType() {
		final var categoryName = "categoryName";
		Category.create();

		// First call should trigger logic in wrapped service class
		metadataService.findTypes(NAMESPACE, MUNICIPALITY_ID, categoryName);

		// Call to delete object should remove key from cache
		metadataService.deleteCategory(NAMESPACE, MUNICIPALITY_ID, categoryName);

		// Second call should trigger logic service class again and not use cache
		metadataService.findTypes(NAMESPACE, MUNICIPALITY_ID, categoryName);

		verify(mock, times(2)).findTypes(NAMESPACE, MUNICIPALITY_ID, categoryName);
		verify(mock).deleteCategory(NAMESPACE, MUNICIPALITY_ID, categoryName);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForContactReason() {
		// First call should trigger logic in wrapped service class
		metadataService.findContactReasons(NAMESPACE, MUNICIPALITY_ID);

		// Call to delete object should remove key from cache
		metadataService.deleteContactReason(1L, NAMESPACE, MUNICIPALITY_ID);

		// Second call should trigger logic service class again and not use cache
		metadataService.findContactReasons(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findContactReasons(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).deleteContactReason(1L, NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForExternalIdType() {
		// First call should trigger logic in wrapped service class
		metadataService.findExternalIdTypes(NAMESPACE, MUNICIPALITY_ID);

		// Call to delete object should remove key from cache
		metadataService.deleteExternalIdType(NAMESPACE, MUNICIPALITY_ID, null);

		// Second call should trigger logic service class again and not use cache
		metadataService.findExternalIdTypes(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findExternalIdTypes(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).deleteExternalIdType(NAMESPACE, MUNICIPALITY_ID, null);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForLabel() {
		// First call should trigger logic in wrapped service class
		metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID);

		// Call to delete object should remove key from cache
		metadataService.deleteLabels(NAMESPACE, MUNICIPALITY_ID);

		// Second call should trigger logic service class again and not use cache
		metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findLabels(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).deleteLabels(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForPatternToLabel() {
		// First call should trigger logic in wrapped service class
		metadataService.patternToLabels(NAMESPACE, MUNICIPALITY_ID, null);

		// Call to create new object should remove key from cache
		metadataService.deleteLabels(NAMESPACE, MUNICIPALITY_ID);

		// Second call should trigger logic service class again and not use cache
		metadataService.patternToLabels(NAMESPACE, MUNICIPALITY_ID, null);

		verify(mock, times(2)).patternToLabels(NAMESPACE, MUNICIPALITY_ID, null);
		verify(mock).deleteLabels(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForRole() {
		// First call should trigger logic in wrapped service class
		metadataService.findRoles(NAMESPACE, MUNICIPALITY_ID);

		// Call to delete object should remove key from cache
		metadataService.deleteRole(NAMESPACE, MUNICIPALITY_ID, null);

		// Second call should trigger logic service class again and not use cache
		metadataService.findRoles(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findRoles(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).deleteRole(NAMESPACE, MUNICIPALITY_ID, null);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForStatus() {
		// First call should trigger logic in wrapped service class
		metadataService.findStatuses(NAMESPACE, MUNICIPALITY_ID);

		// Call to delete object should remove key from cache
		metadataService.deleteStatus(NAMESPACE, MUNICIPALITY_ID, null);

		// Second call should trigger logic service class again and not use cache
		metadataService.findStatuses(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findStatuses(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).deleteStatus(NAMESPACE, MUNICIPALITY_ID, null);
		verifyNoMoreInteractions(mock);
	}
}

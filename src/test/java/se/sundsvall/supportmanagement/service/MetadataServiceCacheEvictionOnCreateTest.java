package se.sundsvall.supportmanagement.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.api.model.metadata.Status;

@ContextConfiguration
@ExtendWith(SpringExtension.class)
class MetadataServiceCacheEvictionOnCreateTest {
	private final static String MUNICIPALITY_ID = "municipalityId";
	private final static String NAMESPACE = "namespace";

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
		// First calls should trigger logic in wrapped service class
		metadataService.findAll(NAMESPACE, MUNICIPALITY_ID);

		switch (type) {
			case "category" -> metadataService.createCategory(NAMESPACE, MUNICIPALITY_ID, Category.create());
			case "contactReason" -> metadataService.createContactReason(NAMESPACE, MUNICIPALITY_ID, ContactReason.create());
			case "externalIdType" -> metadataService.createExternalIdType(NAMESPACE, MUNICIPALITY_ID, ExternalIdType.create());
			case "label" -> metadataService.createLabels(NAMESPACE, MUNICIPALITY_ID, List.of(Label.create()));
			case "role" -> metadataService.createRole(NAMESPACE, MUNICIPALITY_ID, Role.create());
			case "status" -> metadataService.createStatus(NAMESPACE, MUNICIPALITY_ID, Status.create());
			default -> throw new IllegalArgumentException("Unexpected value: " + type);
		}

		// Second calls should trigger logic service class again and not use cache
		metadataService.findAll(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findAll(NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void testCacheEvictionForCategory() {
		final var category = Category.create();

		// First calls should trigger logic in wrapped service class
		metadataService.findCategories(NAMESPACE, MUNICIPALITY_ID);

		// Call to create new object should remove key from cache
		metadataService.createCategory(NAMESPACE, MUNICIPALITY_ID, category);

		// Second calls should trigger logic service class again and not use cache
		metadataService.findCategories(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findCategories(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).createCategory(NAMESPACE, MUNICIPALITY_ID, category);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForType() {
		final var categoryName = "categoryName";
		final var category = Category.create().withName(categoryName);

		// First calls should trigger logic in wrapped service class
		metadataService.findTypes(NAMESPACE, MUNICIPALITY_ID, categoryName);

		// Call to create new object should remove key from cache
		metadataService.createCategory(NAMESPACE, MUNICIPALITY_ID, category);

		// Second calls should trigger logic service class again and not use cache
		metadataService.findTypes(NAMESPACE, MUNICIPALITY_ID, categoryName);

		verify(mock, times(2)).findTypes(NAMESPACE, MUNICIPALITY_ID, categoryName);
		verify(mock).createCategory(NAMESPACE, MUNICIPALITY_ID, category);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForContactReason() {
		final var contactReason = ContactReason.create();

		// First calls should trigger logic in wrapped service class
		metadataService.findContactReasons(NAMESPACE, MUNICIPALITY_ID);

		// Call to create new object should remove key from cache
		metadataService.createContactReason(NAMESPACE, MUNICIPALITY_ID, contactReason);

		// Second calls should trigger logic service class again and not use cache
		metadataService.findContactReasons(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findContactReasons(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).createContactReason(NAMESPACE, MUNICIPALITY_ID, contactReason);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForExternalIdType() {
		final var externalIdType = ExternalIdType.create();

		// First calls should trigger logic in wrapped service class
		metadataService.findExternalIdTypes(NAMESPACE, MUNICIPALITY_ID);

		// Call to create new object should remove key from cache
		metadataService.createExternalIdType(NAMESPACE, MUNICIPALITY_ID, externalIdType);

		// Second calls should trigger logic service class again and not use cache
		metadataService.findExternalIdTypes(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findExternalIdTypes(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).createExternalIdType(NAMESPACE, MUNICIPALITY_ID, externalIdType);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForLabel() {
		final var labels = List.of(Label.create());

		// First calls should trigger logic in wrapped service class
		metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID);

		// Call to create new object should remove key from cache
		metadataService.createLabels(NAMESPACE, MUNICIPALITY_ID, labels);

		// Second calls should trigger logic service class again and not use cache
		metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findLabels(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).createLabels(NAMESPACE, MUNICIPALITY_ID, labels);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForPatternToLabel() {
		final var labels = List.of(Label.create());

		// First calls should trigger logic in wrapped service class
		metadataService.patternToLabels(NAMESPACE, MUNICIPALITY_ID, null);

		// Call to create new object should remove key from cache
		metadataService.createLabels(NAMESPACE, MUNICIPALITY_ID, labels);

		// Second calls should trigger logic service class again and not use cache
		metadataService.patternToLabels(NAMESPACE, MUNICIPALITY_ID, null);

		verify(mock, times(2)).patternToLabels(NAMESPACE, MUNICIPALITY_ID, null);
		verify(mock).createLabels(NAMESPACE, MUNICIPALITY_ID, labels);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForRole() {
		final var role = Role.create();

		// First calls should trigger logic in wrapped service class
		metadataService.findRoles(NAMESPACE, MUNICIPALITY_ID);

		// Call to create new object should remove key from cache
		metadataService.createRole(NAMESPACE, MUNICIPALITY_ID, role);

		// Second calls should trigger logic service class again and not use cache
		metadataService.findRoles(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findRoles(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).createRole(NAMESPACE, MUNICIPALITY_ID, role);
		verifyNoMoreInteractions(mock);
	}

	@Test
	void testCacheEvictionForStatus() {
		final var status = Status.create();

		// First calls should trigger logic in wrapped service class
		metadataService.findStatuses(NAMESPACE, MUNICIPALITY_ID);

		// Call to create new object should remove key from cache
		metadataService.createStatus(NAMESPACE, MUNICIPALITY_ID, status);

		// Second calls should trigger logic service class again and not use cache
		metadataService.findStatuses(NAMESPACE, MUNICIPALITY_ID);

		verify(mock, times(2)).findStatuses(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).createStatus(NAMESPACE, MUNICIPALITY_ID, status);
		verifyNoMoreInteractions(mock);
	}
}

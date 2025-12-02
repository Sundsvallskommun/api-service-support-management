package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.Labels;
import se.sundsvall.supportmanagement.api.model.metadata.MetadataResponse;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;

@ContextConfiguration
@ExtendWith(SpringExtension.class)
class MetadataServiceCacheTest {
	private final static String MUNICIPALITY_ID = "municipalityId";
	private final static String NAMESPACE = "namespace";
	private final static RuntimeException NOT_CACHED_EXCEPTION = new RuntimeException("Result should be cached");

	private static final List<Category> CATEGORIES = List.of(Category.create());
	private static final List<ContactReason> CONTACT_REASONS = List.of(ContactReason.create());
	private static final List<ExternalIdType> EXTERNAL_ID_TYPES = List.of(ExternalIdType.create());
	private static final List<Type> TYPES = List.of(Type.create());
	private static final Labels LABELS = Labels.create();
	private static final List<Role> ROLES = List.of(Role.create());
	private static final List<Status> STATUSES = List.of(Status.create());
	private static final Set<MetadataLabelEntity> METADATA_LABELS = Set.of(MetadataLabelEntity.create());
	private static final MetadataResponse METADATA_RESPONSE = MetadataResponse.create()
		.withCategories(CATEGORIES)
		.withContactReasons(CONTACT_REASONS)
		.withExternalIdTypes(EXTERNAL_ID_TYPES)
		.withLabels(LABELS)
		.withRoles(ROLES)
		.withStatuses(STATUSES);

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

	@Test
	void testCachedFindAll() {
		when(mock.findAll(any(), any()))
			.thenReturn(METADATA_RESPONSE) // On first call, return response
			.thenThrow(NOT_CACHED_EXCEPTION); // If any more calls are received, throw exception

		// First call should trigger logic in wrapped service class
		final var result1 = metadataService.findAll(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).findAll(NAMESPACE, MUNICIPALITY_ID);

		// Second call should go directly to cache and not reach mock
		final var result2 = metadataService.findAll(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(mock);

		// Verify that the result is the same
		assertThat(result1).isSameAs(result2);
	}

	@Test
	void testCachedIsValidated() {
		when(mock.isValidated(any(), any(), any()))
			.thenReturn(true) // On first call, return response
			.thenThrow(NOT_CACHED_EXCEPTION); // If any more calls are received, throw exception

		// First call should trigger logic in wrapped service class
		final var result1 = metadataService.isValidated(NAMESPACE, MUNICIPALITY_ID, EntityType.ROLE);
		verify(mock).isValidated(NAMESPACE, MUNICIPALITY_ID, EntityType.ROLE);

		// Second call should go directly to cache and not reach mock
		final var result2 = metadataService.isValidated(NAMESPACE, MUNICIPALITY_ID, EntityType.ROLE);
		verifyNoMoreInteractions(mock);

		// Verify that the result is the same
		assertThat(result1).isSameAs(result2);
	}

	@Test
	void testCachedFindExternalIdTypes() {
		when(mock.findExternalIdTypes(any(), any()))
			.thenReturn(EXTERNAL_ID_TYPES) // On first call, return response
			.thenThrow(NOT_CACHED_EXCEPTION); // If any more calls are received, throw exception

		// First call should trigger logic in wrapped service class
		final var result1 = metadataService.findExternalIdTypes(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).findExternalIdTypes(NAMESPACE, MUNICIPALITY_ID);

		// Second call should go directly to cache and not reach mock
		final var result2 = metadataService.findExternalIdTypes(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(mock);

		// Verify that the result is the same
		assertThat(result1).isSameAs(result2);
	}

	@Test
	void testCachedFindStatuses() {
		when(mock.findStatuses(any(), any()))
			.thenReturn(STATUSES) // On first call, return response
			.thenThrow(NOT_CACHED_EXCEPTION); // If any more calls are received, throw exception

		// First call should trigger logic in wrapped service class
		final var result1 = metadataService.findStatuses(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).findStatuses(NAMESPACE, MUNICIPALITY_ID);

		// Second call should go directly to cache and not reach mock
		final var result2 = metadataService.findStatuses(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(mock);

		// Verify that the result is the same
		assertThat(result1).isSameAs(result2);
	}

	@Test
	void testCachedFindRoles() {
		when(mock.findRoles(any(), any()))
			.thenReturn(ROLES) // On first call, return response
			.thenThrow(NOT_CACHED_EXCEPTION); // If any more calls are received, throw exception

		// First call should trigger logic in wrapped service class
		final var result1 = metadataService.findRoles(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).findRoles(NAMESPACE, MUNICIPALITY_ID);

		// Second call should go directly to cache and not reach mock
		final var result2 = metadataService.findRoles(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(mock);

		// Verify that the result is the same
		assertThat(result1).isSameAs(result2);
	}

	@Test
	void testCachedFindLabels() {
		when(mock.findLabels(any(), any()))
			.thenReturn(LABELS) // On first call, return response
			.thenThrow(NOT_CACHED_EXCEPTION); // If any more calls are received, throw exception

		// First call should trigger logic in wrapped service class
		final var result1 = metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).findLabels(NAMESPACE, MUNICIPALITY_ID);

		// Second call should go directly to cache and not reach mock
		final var result2 = metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(mock);

		// Verify that the result is the same
		assertThat(result1).isSameAs(result2);
	}

	@Test
	void testCachedPatternToLabels() {
		final var patterns = List.of("pattern");

		when(mock.patternToLabels(any(), any(), any()))
			.thenReturn(METADATA_LABELS)
			.thenThrow(NOT_CACHED_EXCEPTION);

		// First call should trigger logic in wrapped service class
		final var result1 = metadataService.patternToLabels(NAMESPACE, MUNICIPALITY_ID, patterns);
		verify(mock).patternToLabels(NAMESPACE, MUNICIPALITY_ID, patterns);

		// Second call should go directly to cache and not reach mock
		final var result2 = metadataService.patternToLabels(NAMESPACE, MUNICIPALITY_ID, patterns);
		verifyNoMoreInteractions(mock);

		// Verify that the result is the same
		assertThat(result1).isSameAs(result2);
	}

	@Test
	void testCachedFindCategories() {
		when(mock.findCategories(any(), any()))
			.thenReturn(CATEGORIES)
			.thenThrow(NOT_CACHED_EXCEPTION);

		// First call should trigger logic in wrapped service class
		final var result1 = metadataService.findCategories(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).findCategories(NAMESPACE, MUNICIPALITY_ID);

		// Second call should go directly to cache and not reach mock
		final var result2 = metadataService.findCategories(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(mock);

		// Verify that the result is the same
		assertThat(result1).isSameAs(result2);
	}

	@Test
	void testCachedFindTypes() {
		final var category = "category";
		when(mock.findTypes(any(), any(), any()))
			.thenReturn(TYPES)
			.thenThrow(NOT_CACHED_EXCEPTION);

		// First call should trigger logic in wrapped service class
		final var result1 = metadataService.findTypes(NAMESPACE, MUNICIPALITY_ID, category);
		verify(mock).findTypes(NAMESPACE, MUNICIPALITY_ID, category);

		// Second call should go directly to cache and not reach mock
		final var result2 = metadataService.findTypes(NAMESPACE, MUNICIPALITY_ID, category);
		verifyNoMoreInteractions(mock);

		// Verify that the result is the same
		assertThat(result1).isSameAs(result2);
	}

	@Test
	void testCachedFindContactReasons() {
		when(mock.findContactReasons(any(), any()))
			.thenReturn(CONTACT_REASONS)
			.thenThrow(NOT_CACHED_EXCEPTION);

		// First call should trigger logic in wrapped service class
		final var result1 = metadataService.findContactReasons(NAMESPACE, MUNICIPALITY_ID);
		verify(mock).findContactReasons(NAMESPACE, MUNICIPALITY_ID);

		// Second call should go directly to cache and not reach mock
		final var result2 = metadataService.findContactReasons(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(mock);

		// Verify that the result is the same
		assertThat(result1).isSameAs(result2);
	}
}

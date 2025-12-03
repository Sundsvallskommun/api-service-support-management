package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import generated.se.sundsvall.accessmapper.Access.AccessLevelEnum;
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
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

@ContextConfiguration
@ExtendWith(SpringExtension.class)
class AccessMapperServiceCacheTest {
	private static final Set<MetadataLabelEntity> ACCESSIBLE_LABELS = Set.of(MetadataLabelEntity.create().withId("id1"), MetadataLabelEntity.create().withId("id2"));

	private AccessMapperService mock;

	@Autowired
	private AccessMapperService accessMapperService;

	// Provides a mock implementation for the service and a cache manager
	@EnableCaching
	@Configuration
	public static class CachingTestConfig {

		@Bean
		public AccessMapperService accessMapperServiceMockImplementation() {
			return mock(AccessMapperService.class);
		}

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("accessibleLabelsCache");
		}
	}

	@BeforeEach
	void setUp() {
		// AccessMapperService is a proxy around our mock. So, in order to use Mockito validations, we retrieve the actual mock
		// via AopTestUtils.getTargetObject
		mock = AopTestUtils.getTargetObject(accessMapperService);

		// reset(mock) is called between each test because CachingTestConfig only loads once
		reset(mock);

		when(mock.getAccessibleLabels(any(), any(), any(), anyList()))
			.thenReturn(ACCESSIBLE_LABELS) // On first call, return list
			.thenThrow(new RuntimeException("Result should be cached!")); // If any more calls are received, throw exception
	}

	@Test
	void testCaching() {
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var username = "username";
		final var user = Identifier.parse(username + "; type=adaccount");
		final var filter = List.of(AccessLevelEnum.R, AccessLevelEnum.RW);

		// First call should trigger logic in wrapped service class
		final var result1 = accessMapperService.getAccessibleLabels(municipalityId, namespace, user, filter);
		verify(mock).getAccessibleLabels(municipalityId, namespace, user, filter);

		// Second call should go directly to cache and not reach mock
		final var result2 = accessMapperService.getAccessibleLabels(municipalityId, namespace, user, filter);
		verifyNoMoreInteractions(mock);

		// Verify that the result is the same
		assertThat(result1).isSameAs(result2);
	}
}

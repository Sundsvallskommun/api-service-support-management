package se.sundsvall.supportmanagement.service.config;

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
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ContextConfiguration
@ExtendWith(SpringExtension.class)
class NamespaceConfigServiceCacheTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";

	private NamespaceConfigService mock;

	@Autowired
	private NamespaceConfigService namespaceConfigService;

	@Autowired
	private CacheManager cacheManager;

	// Provides a mock implementation for the service and a cache manager
	@EnableCaching
	@Configuration
	public static class CachingTestConfig {

		@Bean
		public NamespaceConfigService namespaceConfigServiceMockImplementation() {
			return mock(NamespaceConfigService.class);
		}

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("namespaceConfigCache");
		}
	}

	@BeforeEach
	void setUp() {
		// NamespaceConfigService is a proxy around our mock, so the mock itself is retrieved to verify against
		mock = AopTestUtils.getTargetObject(namespaceConfigService);

		// The context, and with it the cache, is loaded once for the whole class
		reset(mock);
		cacheManager.getCacheNames().forEach(name -> requireNonNull(cacheManager.getCache(name)).clear());
	}

	@Test
	void isAccessControlActiveIsAnsweredFromCache() {
		// Asked on every request reaching a namespace scoped resource, so it may not hit the database each time.
		when(mock.isAccessControlActive(any(), any()))
			.thenReturn(true)
			.thenThrow(new RuntimeException("Result should be cached!"));

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isTrue();
		verify(mock).isAccessControlActive(NAMESPACE, MUNICIPALITY_ID);

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isTrue();
		verifyNoMoreInteractions(mock);
	}

	@Test
	void createEvictsTheAccessControlAnswer() {
		when(mock.isAccessControlActive(any(), any())).thenReturn(false, true);

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isFalse();
		namespaceConfigService.create(NamespaceConfig.create(), NAMESPACE, MUNICIPALITY_ID);

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isTrue();
	}

	@Test
	void replaceEvictsTheAccessControlAnswer() {
		when(mock.isAccessControlActive(any(), any())).thenReturn(true, false);

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isTrue();
		namespaceConfigService.replace(NamespaceConfig.create(), NAMESPACE, MUNICIPALITY_ID);

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isFalse();
	}

	@Test
	void deleteEvictsTheAccessControlAnswer() {
		when(mock.isAccessControlActive(any(), any())).thenReturn(true, false);

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isTrue();
		namespaceConfigService.delete(NAMESPACE, MUNICIPALITY_ID);

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isFalse();
	}
}

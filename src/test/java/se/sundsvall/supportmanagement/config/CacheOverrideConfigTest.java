package se.sundsvall.supportmanagement.config;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import se.sundsvall.supportmanagement.config.CacheOverrideConfigProperties.CaffeineCache;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CacheOverrideConfigTest {

	@Mock
	private CaffeineCacheManager managerMock;

	@Test
	void testConfig() {
		final var cacheName = "cacheName";
		final var spec = "maximumSize=123, expireAfterWrite=456m";
		final var specOverride = new CaffeineCache();
		specOverride.setCacheName(cacheName);
		specOverride.setSpec(spec);

		CacheOverrideConfig.replaceCaffeineCache(managerMock, specOverride);

		verify(managerMock).removeCache(cacheName);
		verify(managerMock).registerCustomCache(eq(cacheName), ArgumentMatchers.<Cache<Object, Object>>any());
	}
}

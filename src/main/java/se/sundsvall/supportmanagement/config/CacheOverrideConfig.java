package se.sundsvall.supportmanagement.config;

import static java.util.Optional.ofNullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Collections;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheOverrideConfig {

	@Bean
	public CacheManagerCustomizer<CaffeineCacheManager> cacheManagerCustomizer(CacheOverrideConfigProperties properties) {
		return cacheManager -> ofNullable(properties.getSpecOverrides()).orElse(Collections.emptyList())
			.forEach(specOverride -> replaceCaffeineCache(cacheManager, specOverride));
	}

	/**
	 * Replaces default configured cache in manager with a cache using overriding cache settings
	 *
	 * @param cacheManager  current cache manager
	 * @param caffeineCache settings that should override the default cache
	 */
	private void replaceCaffeineCache(CaffeineCacheManager cacheManager, CacheOverrideConfigProperties.CaffeineCache caffeineCache) {
		cacheManager.removeCache(caffeineCache.getCacheName());
		final Cache<Object, Object> cache = Caffeine.from(caffeineCache.getSpec()).build();
		cacheManager.registerCustomCache(caffeineCache.getCacheName(), cache);
	}
}

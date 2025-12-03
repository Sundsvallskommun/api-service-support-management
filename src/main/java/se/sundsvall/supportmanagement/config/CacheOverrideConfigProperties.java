package se.sundsvall.supportmanagement.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.cache.caffeine")
public class CacheOverrideConfigProperties {

	private List<CaffeineCache> specOverrides;

	public List<CaffeineCache> getSpecOverrides() {
		return specOverrides;
	}

	public void setSpecOverrides(List<CaffeineCache> specOverrides) {
		this.specOverrides = specOverrides;
	}

	public static class CaffeineCache {
		private String cacheName;
		private String spec;

		public String getCacheName() {
			return cacheName;
		}

		public void setCacheName(String cacheName) {
			this.cacheName = cacheName;
		}

		public String getSpec() {
			return spec;
		}

		public void setSpec(String spec) {
			this.spec = spec;
		}
	}

}

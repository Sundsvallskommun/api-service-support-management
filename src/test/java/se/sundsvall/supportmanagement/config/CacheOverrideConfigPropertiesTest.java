package se.sundsvall.supportmanagement.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.supportmanagement.ApplicationTest;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationTest
class CacheOverrideConfigPropertiesTest {

	@Autowired
	private CacheOverrideConfigProperties properties;

	@Test
	void testPropertyValues() {
		assertThat(properties).isNotNull();
		assertThat(properties.getSpecOverrides()).hasSize(1).satisfiesExactly(cacheSetting -> {
			assertThat(cacheSetting.getCacheName()).isEqualTo("accessibleLabelsCache");
			assertThat(cacheSetting.getSpec()).isEqualTo("maximumSize=1000, expireAfterWrite=15m");
		});
	}
}

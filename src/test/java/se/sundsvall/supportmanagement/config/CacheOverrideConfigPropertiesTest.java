package se.sundsvall.supportmanagement.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = MOCK)
class CacheOverrideConfigPropertiesTest {

	@Autowired
	private CacheOverrideConfigProperties properties;

	@Test
	void testPropertyValues() {
		assertThat(properties).isNotNull();
		assertThat(properties.getSpecOverrides()).hasSize(1).satisfiesExactly(cacheSetting -> {
			assertThat(cacheSetting.getCacheName()).isEqualTo("accessibleLabelsCache");
			assertThat(cacheSetting.getSpec()).isEqualTo("maximumSize=500, expireAfterWrite=60m");
		});
	}
}

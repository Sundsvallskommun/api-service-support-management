package se.sundsvall.supportmanagement.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@SpringBootTest(webEnvironment = MOCK)
@ActiveProfiles("junit")
class CacheOverrideConfigPropertiesTest {

	@Autowired
	private CacheOverrideConfigProperties properties;

	@Test
	void testPropertyValues() {
		assertThat(properties).isNotNull();
		assertThat(properties.getSpecOverrides()).hasSize(2).satisfiesExactly(
			accessibleLabels -> {
				assertThat(accessibleLabels.getCacheName()).isEqualTo("accessibleLabelsCache");
				assertThat(accessibleLabels.getSpec()).isEqualTo("maximumSize=1000, expireAfterWrite=15m");
			},
			namespaceLabelIds -> {
				assertThat(namespaceLabelIds.getCacheName()).isEqualTo("namespaceLabelIdsCache");
				assertThat(namespaceLabelIds.getSpec()).isEqualTo("maximumSize=200, expireAfterWrite=5m");
			});
	}
}

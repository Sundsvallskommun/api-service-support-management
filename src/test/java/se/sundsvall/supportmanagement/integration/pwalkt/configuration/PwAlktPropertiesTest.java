package se.sundsvall.supportmanagement.integration.pwalkt.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.supportmanagement.ApplicationTest;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationTest
class PwAlktPropertiesTest {

	@Autowired
	private PwAlktProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(10);
	}
}

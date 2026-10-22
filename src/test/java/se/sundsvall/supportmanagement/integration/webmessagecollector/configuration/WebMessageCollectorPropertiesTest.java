package se.sundsvall.supportmanagement.integration.webmessagecollector.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.supportmanagement.ApplicationTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ApplicationTest
class WebMessageCollectorPropertiesTest {

	@Autowired
	private WebMessageCollectorProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
	}

}

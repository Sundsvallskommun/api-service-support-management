package se.sundsvall.supportmanagement.integration.relation.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.supportmanagement.Application;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class RelationPropertiesTest {
	@Autowired
	private RelationProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
	}
}

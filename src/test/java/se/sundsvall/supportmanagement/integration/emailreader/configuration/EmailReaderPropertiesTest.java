package se.sundsvall.supportmanagement.integration.emailreader.configuration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import se.sundsvall.supportmanagement.Application;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class EmailReaderPropertiesTest {

	@Autowired
	private EmailReaderProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
		assertThat(properties.namespace()).isEqualTo("namespace");
		assertThat(properties.municipalityId()).isEqualTo("1234");
	}

}

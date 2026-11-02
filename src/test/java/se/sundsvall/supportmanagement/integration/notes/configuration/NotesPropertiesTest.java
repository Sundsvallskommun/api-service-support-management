package se.sundsvall.supportmanagement.integration.notes.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.supportmanagement.ApplicationTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ApplicationTest
class NotesPropertiesTest {

	@Autowired
	private NotesProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
	}
}

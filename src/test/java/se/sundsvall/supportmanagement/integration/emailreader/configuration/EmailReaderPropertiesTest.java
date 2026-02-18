package se.sundsvall.supportmanagement.integration.emailreader.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.supportmanagement.Application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
		assertThat(properties.errandClosedEmailTemplate()).isEqualTo("""
			Ditt ärende är nu stängt på grund av saknad återkoppling. Vänligen återkom per telefon eller via e-post för att skapa nytt ärende.
			Detta e-postmeddelande går ej att svara på.
			""");
		assertThat(properties.errandClosedEmailSender()).isEqualTo("noreply@sundsvall.se");
	}

}

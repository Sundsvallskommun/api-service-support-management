package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.CoreMatchers.allOf;

class TeliaAceWorkItemEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(TeliaAceWorkItemEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var errandId = "errand-id";
		final var municipalityId = "2281";
		final var namespace = "KONTAKTSUNDSVALL";
		final var fromAddress = "anna.andersson@example.com";
		final var subject = "Nytt ärende i Draken";
		final var contentUrl = "https://draken.sundsvall.se/kontaktsundsvall/arende/KS-123456";
		final var predefinedAgentName = "jep11jep";

		final var bean = TeliaAceWorkItemEntity.create()
			.withErrandId(errandId)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withFromAddress(fromAddress)
			.withSubject(subject)
			.withContentUrl(contentUrl)
			.withPredefinedAgentName(predefinedAgentName);

		assertThat(bean.getErrandId()).isEqualTo(errandId);
		assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(bean.getNamespace()).isEqualTo(namespace);
		assertThat(bean.getFromAddress()).isEqualTo(fromAddress);
		assertThat(bean.getSubject()).isEqualTo(subject);
		assertThat(bean.getContentUrl()).isEqualTo(contentUrl);
		assertThat(bean.getPredefinedAgentName()).isEqualTo(predefinedAgentName);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(TeliaAceWorkItemEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new TeliaAceWorkItemEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testPrePersistSetsCreated() {
		final var bean = TeliaAceWorkItemEntity.create();
		bean.onCreate();
		assertThat(bean.getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(bean).hasAllNullFieldsOrPropertiesExcept("created");
	}
}

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

class EmailDispatchOutboxEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(EmailDispatchOutboxEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var municipalityId = "2281";
		final var namespace = "NAMESPACE-1";
		final var errandId = "errand-id";
		final var errandNumber = "PRH-2022-000001";
		final var subscriberId = "subscriber-id";
		final var recipientEmail = "test@example.com";
		final var identifierType = "AD_ACCOUNT";
		final var identifierValue = "joe01doe";
		final var eventSummary = "Bilaga har skapats";
		final var attempts = 3;
		final var lastAttempted = now();

		final var bean = EmailDispatchOutboxEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withErrandId(errandId)
			.withErrandNumber(errandNumber)
			.withSubscriberId(subscriberId)
			.withRecipientEmail(recipientEmail)
			.withIdentifierType(identifierType)
			.withIdentifierValue(identifierValue)
			.withEventSummary(eventSummary)
			.withAttempts(attempts)
			.withLastAttempted(lastAttempted);

		assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(bean.getNamespace()).isEqualTo(namespace);
		assertThat(bean.getErrandId()).isEqualTo(errandId);
		assertThat(bean.getErrandNumber()).isEqualTo(errandNumber);
		assertThat(bean.getSubscriberId()).isEqualTo(subscriberId);
		assertThat(bean.getRecipientEmail()).isEqualTo(recipientEmail);
		assertThat(bean.getIdentifierType()).isEqualTo(identifierType);
		assertThat(bean.getIdentifierValue()).isEqualTo(identifierValue);
		assertThat(bean.getEventSummary()).isEqualTo(eventSummary);
		assertThat(bean.getAttempts()).isEqualTo(attempts);
		assertThat(bean.getLastAttempted()).isEqualTo(lastAttempted);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(EmailDispatchOutboxEntity.create())
			.hasAllNullFieldsOrPropertiesExcept("attempts");
		assertThat(new EmailDispatchOutboxEntity())
			.hasAllNullFieldsOrPropertiesExcept("attempts");
	}

	@Test
	void testPrePersistSetsCreated() {
		final var bean = EmailDispatchOutboxEntity.create();
		bean.onCreate();
		assertThat(bean.getCreated()).isCloseTo(now(), within(2, SECONDS));
	}
}

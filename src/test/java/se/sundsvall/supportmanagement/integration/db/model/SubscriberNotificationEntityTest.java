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

class SubscriberNotificationEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(SubscriberNotificationEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "notification-id";
		final var identifierType = "adAccount";
		final var identifierValue = "joe01doe";
		final var municipalityId = "2281";
		final var namespace = "NAMESPACE-1";
		final var errandId = "errand-id";
		final var errandNumber = "PRH-2022-000001";
		final var expires = now().plusDays(7);
		final var acknowledged = now();

		final var bean = SubscriberNotificationEntity.create()
			.withId(id)
			.withIdentifierType(identifierType)
			.withIdentifierValue(identifierValue)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withErrandId(errandId)
			.withErrandNumber(errandNumber)
			.withExpires(expires)
			.withAcknowledged(acknowledged);

		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getIdentifierType()).isEqualTo(identifierType);
		assertThat(bean.getIdentifierValue()).isEqualTo(identifierValue);
		assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(bean.getNamespace()).isEqualTo(namespace);
		assertThat(bean.getErrandId()).isEqualTo(errandId);
		assertThat(bean.getErrandNumber()).isEqualTo(errandNumber);
		assertThat(bean.getExpires()).isEqualTo(expires);
		assertThat(bean.getAcknowledged()).isEqualTo(acknowledged);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(SubscriberNotificationEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new SubscriberNotificationEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testPrePersistSetsCreated() {
		final var bean = SubscriberNotificationEntity.create();
		bean.onCreate();
		assertThat(bean.getCreated()).isCloseTo(now(), within(2, SECONDS));
	}

	@Test
	void testPreUpdateSetsModified() {
		final var bean = SubscriberNotificationEntity.create();
		bean.onUpdate();
		assertThat(bean.getModified()).isCloseTo(now(), within(2, SECONDS));
	}
}

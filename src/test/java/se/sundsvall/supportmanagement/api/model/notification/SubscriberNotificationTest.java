package se.sundsvall.supportmanagement.api.model.notification;

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class SubscriberNotificationTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(SubscriberNotification.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "notification-id";
		final var created = now();
		final var modified = now().plusMinutes(1);
		final var identifierType = "adAccount";
		final var identifierValue = "joe01doe";
		final var errandId = "errand-id";
		final var errandNumber = "PRH-2022-000001";
		final var expires = now().plusDays(7);
		final var acknowledged = now().plusHours(1);

		final var bean = SubscriberNotification.create()
			.withId(id)
			.withCreated(created)
			.withModified(modified)
			.withIdentifierType(identifierType)
			.withIdentifierValue(identifierValue)
			.withErrandId(errandId)
			.withErrandNumber(errandNumber)
			.withExpires(expires)
			.withAcknowledged(acknowledged);

		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getModified()).isEqualTo(modified);
		assertThat(bean.getIdentifierType()).isEqualTo(identifierType);
		assertThat(bean.getIdentifierValue()).isEqualTo(identifierValue);
		assertThat(bean.getErrandId()).isEqualTo(errandId);
		assertThat(bean.getErrandNumber()).isEqualTo(errandNumber);
		assertThat(bean.getExpires()).isEqualTo(expires);
		assertThat(bean.getAcknowledged()).isEqualTo(acknowledged);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(SubscriberNotification.create()).hasAllNullFieldsOrProperties();
		assertThat(new SubscriberNotification()).hasAllNullFieldsOrProperties();
	}
}

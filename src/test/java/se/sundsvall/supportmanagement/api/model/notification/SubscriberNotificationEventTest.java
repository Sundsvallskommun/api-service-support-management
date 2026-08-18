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

class SubscriberNotificationEventTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(SubscriberNotificationEvent.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var created = now();
		final var eventType = "UPDATE";
		final var description = "Bilaga har skapats";
		final var subType = "ATTACHMENT";

		final var bean = SubscriberNotificationEvent.create()
			.withCreated(created)
			.withEventType(eventType)
			.withDescription(description)
			.withSubType(subType);

		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getEventType()).isEqualTo(eventType);
		assertThat(bean.getDescription()).isEqualTo(description);
		assertThat(bean.getSubType()).isEqualTo(subType);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(SubscriberNotificationEvent.create()).hasAllNullFieldsOrProperties();
		assertThat(new SubscriberNotificationEvent()).hasAllNullFieldsOrProperties();
	}
}

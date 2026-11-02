package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.CoreMatchers.allOf;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.SMS;

class NotificationDispatchOutboxEntityTest {

	@BeforeAll
	static void setup() {
		final var random = new Random();
		registerValueGenerator(() -> now().plusDays(random.nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(NotificationDispatchOutboxEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("subscriber"),
			hasValidBeanEqualsExcluding("subscriber"),
			hasValidBeanToStringExcluding("subscriber")));
	}

	@Test
	void testBuilderMethods() {
		final var id = "outbox-id";
		final var subscriber = SubscriberEntity.create().withId("subscriber-id");
		final var channel = SMS;
		final var errandId = "errand-id";
		final var errandNumber = "PRH-2022-000001";
		final var events = List.of(NotificationDispatchOutboxEventEmbeddable.create().withEventId("event-id").withDescription("Ärendet har uppdaterats"));
		final var created = now();

		final var bean = NotificationDispatchOutboxEntity.create()
			.withId(id)
			.withSubscriber(subscriber)
			.withChannel(channel)
			.withErrandId(errandId)
			.withErrandNumber(errandNumber)
			.withEvents(events)
			.withCreated(created);

		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getSubscriber()).isEqualTo(subscriber);
		assertThat(bean.getChannel()).isEqualTo(channel);
		assertThat(bean.getErrandId()).isEqualTo(errandId);
		assertThat(bean.getErrandNumber()).isEqualTo(errandNumber);
		assertThat(bean.getEvents()).isEqualTo(events);
		assertThat(bean.getCreated()).isEqualTo(created);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NotificationDispatchOutboxEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new NotificationDispatchOutboxEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testPrePersistSetsCreated() {
		final var bean = NotificationDispatchOutboxEntity.create();
		bean.onCreate();
		assertThat(bean.getCreated()).isCloseTo(now(), within(2, SECONDS));
	}
}

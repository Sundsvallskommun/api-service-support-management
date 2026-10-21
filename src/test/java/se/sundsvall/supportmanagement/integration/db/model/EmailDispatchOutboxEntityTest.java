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

class EmailDispatchOutboxEntityTest {

	@BeforeAll
	static void setup() {
		final var random = new Random();
		registerValueGenerator(() -> now().plusDays(random.nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(EmailDispatchOutboxEntity.class, allOf(
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
		final var errandId = "errand-id";
		final var errandNumber = "PRH-2022-000001";
		final var events = List.of(EmailDispatchOutboxEventEmbeddable.create().withEventId("event-id").withDescription("Ärendet har uppdaterats"));
		final var created = now();

		final var bean = EmailDispatchOutboxEntity.create()
			.withId(id)
			.withSubscriber(subscriber)
			.withErrandId(errandId)
			.withErrandNumber(errandNumber)
			.withEvents(events)
			.withCreated(created);

		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getSubscriber()).isEqualTo(subscriber);
		assertThat(bean.getErrandId()).isEqualTo(errandId);
		assertThat(bean.getErrandNumber()).isEqualTo(errandNumber);
		assertThat(bean.getEvents()).isEqualTo(events);
		assertThat(bean.getCreated()).isEqualTo(created);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(EmailDispatchOutboxEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new EmailDispatchOutboxEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testPrePersistSetsCreated() {
		final var bean = EmailDispatchOutboxEntity.create();
		bean.onCreate();
		assertThat(bean.getCreated()).isCloseTo(now(), within(2, SECONDS));
	}
}

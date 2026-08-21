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

class NotificationDispatchEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(NotificationDispatchEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var eventId = "event-id";
		final var requestGroupId = "request-group-id";
		final var errandId = "errand-id";
		final var municipalityId = "2281";
		final var namespace = "NAMESPACE-1";
		final var eventType = "CREATE";
		final var description = "Bilaga har skapats";
		final var subType = "ATTACHMENT";
		final var executingUserId = "joe01doe";

		final var bean = NotificationDispatchEntity.create()
			.withEventId(eventId)
			.withRequestGroupId(requestGroupId)
			.withErrandId(errandId)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withEventType(eventType)
			.withDescription(description)
			.withSubType(subType)
			.withExecutingUserId(executingUserId);

		assertThat(bean.getEventId()).isEqualTo(eventId);
		assertThat(bean.getRequestGroupId()).isEqualTo(requestGroupId);
		assertThat(bean.getErrandId()).isEqualTo(errandId);
		assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(bean.getNamespace()).isEqualTo(namespace);
		assertThat(bean.getEventType()).isEqualTo(eventType);
		assertThat(bean.getDescription()).isEqualTo(description);
		assertThat(bean.getSubType()).isEqualTo(subType);
		assertThat(bean.getExecutingUserId()).isEqualTo(executingUserId);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NotificationDispatchEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new NotificationDispatchEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testPrePersistSetsCreated() {
		final var bean = NotificationDispatchEntity.create();
		bean.onCreate();
		assertThat(bean.getCreated()).isCloseTo(now(), within(2, SECONDS));
	}
}

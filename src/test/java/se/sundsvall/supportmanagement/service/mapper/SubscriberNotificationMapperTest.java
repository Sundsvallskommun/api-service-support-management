package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchEntity;
import se.sundsvall.supportmanagement.integration.db.model.SubscriberNotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.SubscriberNotificationEventEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

class SubscriberNotificationMapperTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String ERRAND_ID = "errand-id";
	private static final String ERRAND_NUMBER = "PRH-2022-000001";
	private static final String IDENTIFIER_TYPE = "adAccount";
	private static final String IDENTIFIER_VALUE = "joe01doe";
	private static final String EVENT_TYPE = "UPDATE";
	private static final String DESCRIPTION = "Bilaga har skapats";
	private static final String SUB_TYPE = "ATTACHMENT";
	private static final int NOTIFICATION_TTL_IN_DAYS = 30;

	private static SubscriberEntity createSubscriber() {
		return SubscriberEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withIdentifier(IdentifierEmbeddable.create()
				.withType(IDENTIFIER_TYPE)
				.withValue(IDENTIFIER_VALUE));
	}

	@Test
	void testToEntity() {
		final var eventEntity = SubscriberNotificationEventEntity.create().withEventType(EVENT_TYPE);
		final var events = List.of(eventEntity);

		final var result = SubscriberNotificationMapper.toEntity(ERRAND_ID, ERRAND_NUMBER, createSubscriber(), NOTIFICATION_TTL_IN_DAYS, events);

		assertThat(result.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(result.getIdentifierType()).isEqualTo(IDENTIFIER_TYPE);
		assertThat(result.getIdentifierValue()).isEqualTo(IDENTIFIER_VALUE);
		assertThat(result.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(result.getErrandNumber()).isEqualTo(ERRAND_NUMBER);
		assertThat(result.getExpires()).isCloseTo(now().plusDays(NOTIFICATION_TTL_IN_DAYS), within(2, SECONDS));
		assertThat(result.getEvents()).isEqualTo(events);
	}

	@Test
	void testToEventEntity() {
		final var result = SubscriberNotificationMapper.toEventEntity(NotificationDispatchEntity.create()
			.withEventType(EVENT_TYPE)
			.withDescription(DESCRIPTION)
			.withSubType(SUB_TYPE));

		assertThat(result.getEventType()).isEqualTo(EVENT_TYPE);
		assertThat(result.getDescription()).isEqualTo(DESCRIPTION);
		assertThat(result.getSubType()).isEqualTo(SUB_TYPE);
		assertThat(result.getId()).isNull();
		assertThat(result.getCreated()).isNull();
	}

	@Test
	void testToEventEntities() {
		final var result = SubscriberNotificationMapper.toEventEntities(List.of(
			NotificationDispatchEntity.create().withEventType(EVENT_TYPE).withDescription(DESCRIPTION).withSubType(SUB_TYPE),
			NotificationDispatchEntity.create().withEventType("CREATE")));

		assertThat(result)
			.extracting(SubscriberNotificationEventEntity::getEventType, SubscriberNotificationEventEntity::getDescription, SubscriberNotificationEventEntity::getSubType)
			.containsExactly(
				tuple(EVENT_TYPE, DESCRIPTION, SUB_TYPE),
				tuple("CREATE", null, null));
	}

	@Test
	void testToEventEntitiesReturnsMutableListForHibernate() {
		final var result = SubscriberNotificationMapper.toEventEntities(List.of(NotificationDispatchEntity.create().withEventType(EVENT_TYPE)));

		assertThatCode(() -> result.add(SubscriberNotificationEventEntity.create())).doesNotThrowAnyException();
	}

	@Test
	void testToEventEntitiesWithNull() {
		assertThat(SubscriberNotificationMapper.toEventEntities(null)).isEmpty();
	}

	@Test
	void testToModel() {
		final var created = now();
		final var modified = now().plusMinutes(1);
		final var expires = now().plusDays(NOTIFICATION_TTL_IN_DAYS);
		final var acknowledged = now().plusHours(1);
		final var eventCreated = now();
		final var eventEntity = SubscriberNotificationEventEntity.create()
			.withCreated(eventCreated)
			.withEventType(EVENT_TYPE)
			.withDescription(DESCRIPTION)
			.withSubType(SUB_TYPE);

		final var entity = SubscriberNotificationEntity.create()
			.withId("notification-id")
			.withCreated(created)
			.withModified(modified)
			.withIdentifierType(IDENTIFIER_TYPE)
			.withIdentifierValue(IDENTIFIER_VALUE)
			.withErrandId(ERRAND_ID)
			.withErrandNumber(ERRAND_NUMBER)
			.withExpires(expires)
			.withAcknowledged(acknowledged)
			.withEvents(List.of(eventEntity));

		final var result = SubscriberNotificationMapper.toModel(entity);

		assertThat(result.getId()).isEqualTo("notification-id");
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
		assertThat(result.getIdentifierType()).isEqualTo(IDENTIFIER_TYPE);
		assertThat(result.getIdentifierValue()).isEqualTo(IDENTIFIER_VALUE);
		assertThat(result.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(result.getErrandNumber()).isEqualTo(ERRAND_NUMBER);
		assertThat(result.getExpires()).isEqualTo(expires);
		assertThat(result.getAcknowledged()).isEqualTo(acknowledged);
		assertThat(result.getEvents()).hasSize(1);

		final var event = result.getEvents().getFirst();
		assertThat(event.getCreated()).isEqualTo(eventCreated);
		assertThat(event.getEventType()).isEqualTo(EVENT_TYPE);
		assertThat(event.getDescription()).isEqualTo(DESCRIPTION);
		assertThat(event.getSubType()).isEqualTo(SUB_TYPE);
	}

	@Test
	void testToModelWithNullEvents() {
		final var entity = SubscriberNotificationEntity.create()
			.withId("notification-id")
			.withIdentifierType(IDENTIFIER_TYPE)
			.withIdentifierValue(IDENTIFIER_VALUE)
			.withErrandId(ERRAND_ID);

		final var result = SubscriberNotificationMapper.toModel(entity);

		assertThat(result.getId()).isEqualTo("notification-id");
		assertThat(result.getEvents()).isNull();
	}

	@Test
	void testToModelWithEmptyEvents() {
		final var entity = SubscriberNotificationEntity.create()
			.withId("notification-id")
			.withIdentifierType(IDENTIFIER_TYPE)
			.withIdentifierValue(IDENTIFIER_VALUE)
			.withErrandId(ERRAND_ID)
			.withEvents(List.of());

		final var result = SubscriberNotificationMapper.toModel(entity);

		assertThat(result.getEvents()).isEmpty();
	}
}

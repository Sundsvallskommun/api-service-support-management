package se.sundsvall.supportmanagement.service.mapper;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.SubscriberNotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.SubscriberNotificationEventEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriberNotificationMapperTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE";
	private static final String ERRAND_ID = "errand-id";
	private static final String ERRAND_NUMBER = "KC-23010001";
	private static final String IDENTIFIER_TYPE = "adAccount";
	private static final String IDENTIFIER_VALUE = "joe01doe";
	private static final String EVENT_TYPE = "UPDATE";
	private static final String DESCRIPTION = "Bilaga har skapats";
	private static final String SUB_TYPE = "ATTACHMENT";

	@Test
	void toEntity() {
		final var subscriber = SubscriberEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withIdentifier(IdentifierEmbeddable.create().withType(IDENTIFIER_TYPE).withValue(IDENTIFIER_VALUE));
		final var eventEntity = SubscriberNotificationEventEntity.create().withEventType(EVENT_TYPE);
		final var events = List.of(eventEntity);

		final var result = SubscriberNotificationMapper.toEntity(ERRAND_ID, ERRAND_NUMBER, subscriber, 30, events);

		assertThat(result.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(result.getIdentifierType()).isEqualTo(IDENTIFIER_TYPE);
		assertThat(result.getIdentifierValue()).isEqualTo(IDENTIFIER_VALUE);
		assertThat(result.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(result.getErrandNumber()).isEqualTo(ERRAND_NUMBER);
		assertThat(result.getExpires()).isAfter(OffsetDateTime.now());
		assertThat(result.getEvents()).containsExactly(eventEntity);
	}

	@Test
	void toEventEntity() {
		final var result = SubscriberNotificationMapper.toEventEntity(EVENT_TYPE, DESCRIPTION, SUB_TYPE);

		assertThat(result.getEventType()).isEqualTo(EVENT_TYPE);
		assertThat(result.getDescription()).isEqualTo(DESCRIPTION);
		assertThat(result.getSubType()).isEqualTo(SUB_TYPE);
	}

	@Test
	void toModel() {
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var expires = OffsetDateTime.now().plusDays(30);
		final var acknowledged = OffsetDateTime.now().minusHours(1);
		final var eventCreated = OffsetDateTime.now().minusMinutes(5);
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
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
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
		assertThat(result.getEvents().getFirst().getCreated()).isEqualTo(eventCreated);
		assertThat(result.getEvents().getFirst().getEventType()).isEqualTo(EVENT_TYPE);
		assertThat(result.getEvents().getFirst().getDescription()).isEqualTo(DESCRIPTION);
		assertThat(result.getEvents().getFirst().getSubType()).isEqualTo(SUB_TYPE);
	}

	@Test
	void toModel_withNullEvents() {
		final var entity = SubscriberNotificationEntity.create()
			.withId("notification-id")
			.withErrandId(ERRAND_ID);

		final var result = SubscriberNotificationMapper.toModel(entity);

		assertThat(result.getId()).isEqualTo("notification-id");
		assertThat(result.getEvents()).isNull();
	}
}

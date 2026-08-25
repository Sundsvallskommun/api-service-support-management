package se.sundsvall.supportmanagement.integration.db.model;

import org.junit.jupiter.api.Test;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MessagingOutboxEntityTest {

	@Test
	void hasValidBuilderMethods() {
		final var id = "id";
		final var municipalityId = "2281";
		final var messageType = "EMAIL";
		final var payload = "{\"subject\":\"test\"}";
		final var created = now().minusDays(1);
		final var retryCount = 2;
		final var nextRetryAt = now().plusMinutes(10);
		final var deadLetter = true;

		final var entity = MessagingOutboxEntity.create()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withMessageType(messageType)
			.withPayload(payload)
			.withCreated(created);

		entity.setRetryCount(retryCount);
		entity.setNextRetryAt(nextRetryAt);
		entity.setDeadLetter(deadLetter);

		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getMessageType()).isEqualTo(messageType);
		assertThat(entity.getPayload()).isEqualTo(payload);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getRetryCount()).isEqualTo(retryCount);
		assertThat(entity.getNextRetryAt()).isEqualTo(nextRetryAt);
		assertThat(entity.isDeadLetter()).isEqualTo(deadLetter);
	}

	@Test
	void testEqualsHashCodeToString() {
		final var entity1 = MessagingOutboxEntity.create()
			.withId("id-1")
			.withMunicipalityId("2281")
			.withMessageType("EMAIL")
			.withPayload("payload");
		final var entity2 = MessagingOutboxEntity.create()
			.withId("id-1")
			.withMunicipalityId("2281")
			.withMessageType("EMAIL")
			.withPayload("payload");
		final var entity3 = MessagingOutboxEntity.create()
			.withId("id-2")
			.withMunicipalityId("2281")
			.withMessageType("EMAIL")
			.withPayload("payload");

		assertThat(entity1).isEqualTo(entity2).hasSameHashCodeAs(entity2);
		assertThat(entity1).isNotEqualTo(entity3);
		assertThat(entity1.toString()).contains("id-1", "2281", "EMAIL");
	}

	@Test
	void testOnCreate() {
		final var entity = MessagingOutboxEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created", "retryCount", "deadLetter");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(MessagingOutboxEntity.create()).hasAllNullFieldsOrPropertiesExcept("retryCount", "deadLetter");
		assertThat(new MessagingOutboxEntity()).hasAllNullFieldsOrPropertiesExcept("retryCount", "deadLetter");
	}
}

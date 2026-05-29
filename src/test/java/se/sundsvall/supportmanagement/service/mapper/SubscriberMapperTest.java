package se.sundsvall.supportmanagement.service.mapper;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.api.model.subscriber.EventFilter;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannel;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType;
import se.sundsvall.supportmanagement.api.model.subscriber.Subscriber;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.EventFilterEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.NotificationChannelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriberMapperTest {

	@Test
	void toSubscriberEntityCopiesAllFields() {
		final var subscriber = Subscriber.create()
			.withName("Servicedesk")
			.withIdentifier(Identifier.create().withType("adAccount").withValue("joe01doe"))
			.withChannels(List.of(NotificationChannel.create().withType(NotificationChannelType.EMAIL).withDestination("joe@example.com")))
			.withEventFilters(List.of(EventFilter.create().withType("UPDATE").withSubtype("ATTACHMENT")))
			.withPausedFrom(OffsetDateTime.parse("2026-06-01T00:00:00+02:00"))
			.withPausedUntil(OffsetDateTime.parse("2026-06-30T00:00:00+02:00"));

		final var entity = SubscriberMapper.toSubscriberEntity("2281", "ns", subscriber);

		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getNamespace()).isEqualTo("ns");
		assertThat(entity.getName()).isEqualTo("Servicedesk");
		assertThat(entity.getIdentifier()).isEqualTo(IdentifierEmbeddable.create().withType("adAccount").withValue("joe01doe"));
		assertThat(entity.getChannels()).containsExactly(
			NotificationChannelEmbeddable.create()
				.withType(se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.EMAIL)
				.withDestination("joe@example.com"));
		assertThat(entity.getEventFilters()).containsExactly(EventFilterEmbeddable.create().withType("UPDATE").withSubtype("ATTACHMENT"));
		assertThat(entity.getPausedFrom()).isEqualTo(subscriber.getPausedFrom());
		assertThat(entity.getPausedUntil()).isEqualTo(subscriber.getPausedUntil());
		// createdBy is set by service, not mapper
		assertThat(entity.getCreatedBy()).isNull();
	}

	@Test
	void toSubscriberEntityReturnsNullForNullInput() {
		assertThat(SubscriberMapper.toSubscriberEntity("2281", "ns", null)).isNull();
	}

	@Test
	void toSubscriberMapsEntityIncludingCount() {
		final var entity = SubscriberEntity.create()
			.withId("id-1")
			.withName("Servicedesk")
			.withIdentifier(IdentifierEmbeddable.create().withType("adAccount").withValue("joe01doe"))
			.withChannels(List.of(NotificationChannelEmbeddable.create()
				.withType(se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.SMS)
				.withDestination("+46700000000")))
			.withEventFilters(List.of(EventFilterEmbeddable.create().withType("UPDATE").withSubtype(null)))
			.withCreated(OffsetDateTime.parse("2026-05-01T12:00:00+02:00"))
			.withModified(OffsetDateTime.parse("2026-05-02T12:00:00+02:00"))
			.withCreatedBy(IdentifierEmbeddable.create().withType("adAccount").withValue("adm01"));

		final var dto = SubscriberMapper.toSubscriber(entity, 5L);

		assertThat(dto.getId()).isEqualTo("id-1");
		assertThat(dto.getName()).isEqualTo("Servicedesk");
		assertThat(dto.getIdentifier()).isEqualTo(Identifier.create().withType("adAccount").withValue("joe01doe"));
		assertThat(dto.getChannels()).containsExactly(NotificationChannel.create().withType(NotificationChannelType.SMS).withDestination("+46700000000"));
		assertThat(dto.getEventFilters()).containsExactly(EventFilter.create().withType("UPDATE"));
		assertThat(dto.getCreated()).isEqualTo(entity.getCreated());
		assertThat(dto.getModified()).isEqualTo(entity.getModified());
		assertThat(dto.getCreatedBy()).isEqualTo(Identifier.create().withType("adAccount").withValue("adm01"));
		assertThat(dto.getSubscriptionCount()).isEqualTo(5);
	}

	@Test
	void toSubscriberWithNullCountLeavesFieldUnset() {
		final var entity = SubscriberEntity.create().withId("id-1");
		final var dto = SubscriberMapper.toSubscriber(entity, null);
		assertThat(dto.getSubscriptionCount()).isNull();
	}

	@Test
	void toSubscriberReturnsNullForNullInput() {
		assertThat(SubscriberMapper.toSubscriber(null, 1L)).isNull();
	}

	@Test
	void applyPatchOnlyUpdatesNonNullFields() {
		final var entity = SubscriberEntity.create()
			.withId("id-1")
			.withName("old")
			.withIdentifier(IdentifierEmbeddable.create().withType("adAccount").withValue("joe01doe"))
			.withChannels(List.of(NotificationChannelEmbeddable.create()
				.withType(se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.INTERNAL)));

		SubscriberMapper.applyPatch(entity, Subscriber.create().withName("new"));

		assertThat(entity.getName()).isEqualTo("new");
		// Identifier left untouched
		assertThat(entity.getIdentifier().getValue()).isEqualTo("joe01doe");
		// Channels left untouched (patch had null channels)
		assertThat(entity.getChannels()).hasSize(1);
	}

	@Test
	void applyPatchWithEmptyChannelsClearsThem() {
		final var entity = SubscriberEntity.create()
			.withChannels(List.of(NotificationChannelEmbeddable.create()
				.withType(se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.EMAIL)));

		SubscriberMapper.applyPatch(entity, Subscriber.create().withChannels(List.of()));

		assertThat(entity.getChannels()).isEmpty();
	}

	@Test
	void applyPatchIgnoresIdentifierOnDto() {
		final var entity = SubscriberEntity.create()
			.withIdentifier(IdentifierEmbeddable.create().withType("adAccount").withValue("original"));

		SubscriberMapper.applyPatch(entity, Subscriber.create()
			.withIdentifier(Identifier.create().withType("partyId").withValue("new-party-id")));

		assertThat(entity.getIdentifier().getType()).isEqualTo("adAccount");
		assertThat(entity.getIdentifier().getValue()).isEqualTo("original");
	}

	@Test
	void applyPatchHandlesNullInputsGracefully() {
		final var entity = SubscriberEntity.create().withName("a");
		SubscriberMapper.applyPatch(entity, null);
		SubscriberMapper.applyPatch(null, Subscriber.create().withName("b"));
		assertThat(entity.getName()).isEqualTo("a");
	}

	@Test
	void applyPatchSetsPauseWindow() {
		final var entity = SubscriberEntity.create().withName("a");
		final var from = OffsetDateTime.parse("2026-06-01T00:00:00+02:00");
		final var until = OffsetDateTime.parse("2026-06-30T00:00:00+02:00");

		SubscriberMapper.applyPatch(entity, Subscriber.create().withPausedFrom(from).withPausedUntil(until));

		assertThat(entity.getPausedFrom()).isEqualTo(from);
		assertThat(entity.getPausedUntil()).isEqualTo(until);
	}

	@Test
	void applyPatchSetsOnlyPausedFromWhenUntilIsNull() {
		final var existingUntil = OffsetDateTime.parse("2026-06-30T00:00:00+02:00");
		final var entity = SubscriberEntity.create().withPausedUntil(existingUntil);
		final var newFrom = OffsetDateTime.parse("2026-06-15T00:00:00+02:00");

		SubscriberMapper.applyPatch(entity, Subscriber.create().withPausedFrom(newFrom));

		assertThat(entity.getPausedFrom()).isEqualTo(newFrom);
		assertThat(entity.getPausedUntil()).isEqualTo(existingUntil);
	}

	// Regression guard: Hibernate must be able to mutate @ElementCollection @OrderColumn lists
	// during update flush. Stream.toList() returns an immutable list which would cause
	// UnsupportedOperationException (translated to 501 NOT_IMPLEMENTED by dept44). See test07_updateSubscriber.
	@Test
	void toSubscriberEntityProducesMutableEmbeddableLists() {
		final var subscriber = Subscriber.create()
			.withIdentifier(Identifier.create().withType("adAccount").withValue("joe01doe"))
			.withChannels(List.of(NotificationChannel.create().withType(NotificationChannelType.INTERNAL)))
			.withEventFilters(List.of(EventFilter.create().withType("UPDATE")));

		final var entity = SubscriberMapper.toSubscriberEntity("2281", "NAMESPACE-1", subscriber);

		assertThat(entity.getChannels()).isInstanceOf(java.util.ArrayList.class);
		assertThat(entity.getEventFilters()).isInstanceOf(java.util.ArrayList.class);
		// Verify they actually accept mutation (catches future use of Stream.toList()).
		entity.getChannels().add(NotificationChannelEmbeddable.create()
			.withType(se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.EMAIL));
		entity.getEventFilters().add(EventFilterEmbeddable.create().withType("CREATE"));
		assertThat(entity.getChannels()).hasSize(2);
		assertThat(entity.getEventFilters()).hasSize(2);
	}

	@Test
	void applyPatchProducesMutableEmbeddableLists() {
		final var entity = SubscriberEntity.create();
		final var patch = Subscriber.create()
			.withChannels(List.of(NotificationChannel.create().withType(NotificationChannelType.INTERNAL)))
			.withEventFilters(List.of(EventFilter.create().withType("UPDATE")));

		SubscriberMapper.applyPatch(entity, patch);

		assertThat(entity.getChannels()).isInstanceOf(java.util.ArrayList.class);
		assertThat(entity.getEventFilters()).isInstanceOf(java.util.ArrayList.class);
		entity.getChannels().clear();
		entity.getEventFilters().clear();
		assertThat(entity.getChannels()).isEmpty();
		assertThat(entity.getEventFilters()).isEmpty();
	}
}

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
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;
import static se.sundsvall.dept44.support.Identifier.Type.PARTY_ID;

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
	void toSubscriberWithoutCountSetsNull() {
		final var entity = SubscriberEntity.create().withId("id-1");
		final var dto = SubscriberMapper.toSubscriber(entity);
		assertThat(dto.getSubscriptionCount()).isNull();
	}

	@Test
	void toSubscriberReturnsNullForNullInput() {
		assertThat(SubscriberMapper.toSubscriber(null)).isNull();
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
	void fromExecutingUserMapsAdAccount() {
		final var emb = SubscriberMapper.fromExecutingUser(
			se.sundsvall.dept44.support.Identifier.create().withType(AD_ACCOUNT).withValue("joe01doe"));

		assertThat(emb).isEqualTo(IdentifierEmbeddable.create().withType("adAccount").withValue("joe01doe"));
	}

	@Test
	void fromExecutingUserMapsPartyId() {
		final var emb = SubscriberMapper.fromExecutingUser(
			se.sundsvall.dept44.support.Identifier.create().withType(PARTY_ID).withValue("98c7b451-a14a-4f9f-91da-8834ba01eb81"));

		assertThat(emb).isEqualTo(IdentifierEmbeddable.create().withType("partyId").withValue("98c7b451-a14a-4f9f-91da-8834ba01eb81"));
	}

	@Test
	void fromExecutingUserReturnsNullForNull() {
		assertThat(SubscriberMapper.fromExecutingUser(null)).isNull();
	}

	@Test
	void fromExecutingUserReturnsNullWhenTypeMissing() {
		assertThat(SubscriberMapper.fromExecutingUser(
			se.sundsvall.dept44.support.Identifier.create().withValue("joe01doe"))).isNull();
	}
}

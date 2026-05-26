package se.sundsvall.supportmanagement.service.mapper;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.api.model.subscriber.EventFilter;
import se.sundsvall.supportmanagement.api.model.subscription.Subscription;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTarget;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionTargetType;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.EventFilterEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriberEntity;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionTargetType.ERRAND;
import static se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionTargetType.NAMESPACE;

class SubscriptionMapperTest {

	@Test
	void toSubscriptionEntityForErrandTarget() {
		final var subscriber = SubscriberEntity.create().withId("sub-1");
		final var errand = new ErrandEntity().withId("errand-1");
		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.ERRAND).withId("errand-1"))
			.withEventFilters(List.of(EventFilter.create().withType("UPDATE")))
			.withExpiresAt(OffsetDateTime.parse("2026-12-31T23:59:59+02:00"));

		final var entity = SubscriptionMapper.toSubscriptionEntity(subscriber, errand, dto);

		assertThat(entity.getSubscriber()).isSameAs(subscriber);
		assertThat(entity.getErrand()).isSameAs(errand);
		assertThat(entity.getTargetType()).isEqualTo(ERRAND);
		assertThat(entity.getEventFilters()).containsExactly(EventFilterEmbeddable.create().withType("UPDATE"));
		assertThat(entity.getExpiresAt()).isEqualTo(dto.getExpiresAt());
	}

	@Test
	void toSubscriptionEntityForNamespaceTarget() {
		final var subscriber = SubscriberEntity.create().withId("sub-1");
		final var dto = Subscription.create()
			.withTarget(SubscriptionTarget.create().withType(SubscriptionTargetType.NAMESPACE));

		final var entity = SubscriptionMapper.toSubscriptionEntity(subscriber, null, dto);

		assertThat(entity.getErrand()).isNull();
		assertThat(entity.getTargetType()).isEqualTo(NAMESPACE);
	}

	@Test
	void toSubscriptionEntityReturnsNullForNull() {
		assertThat(SubscriptionMapper.toSubscriptionEntity(null, null, null)).isNull();
	}

	@Test
	void toSubscriptionMapsErrandTarget() {
		final var errand = new ErrandEntity().withId("errand-1");
		final var entity = SubscriptionEntity.create()
			.withId("sub-1")
			.withTargetType(ERRAND)
			.withErrand(errand)
			.withEventFilters(List.of(EventFilterEmbeddable.create().withType("UPDATE")))
			.withCreated(OffsetDateTime.now())
			.withCreatedBy(IdentifierEmbeddable.create().withType("adAccount").withValue("adm01"));

		final var dto = SubscriptionMapper.toSubscription(entity);

		assertThat(dto.getId()).isEqualTo("sub-1");
		assertThat(dto.getTarget().getType()).isEqualTo(SubscriptionTargetType.ERRAND);
		assertThat(dto.getTarget().getId()).isEqualTo("errand-1");
		assertThat(dto.getEventFilters()).containsExactly(EventFilter.create().withType("UPDATE"));
		assertThat(dto.getCreatedBy()).isEqualTo(Identifier.create().withType("adAccount").withValue("adm01"));
	}

	@Test
	void toSubscriptionMapsNamespaceTarget() {
		final var entity = SubscriptionEntity.create().withId("sub-2").withTargetType(NAMESPACE);

		final var dto = SubscriptionMapper.toSubscription(entity);

		assertThat(dto.getTarget().getType()).isEqualTo(SubscriptionTargetType.NAMESPACE);
		assertThat(dto.getTarget().getId()).isNull();
	}

	@Test
	void toSubscriptionReturnsNullForNull() {
		assertThat(SubscriptionMapper.toSubscription(null)).isNull();
	}
}

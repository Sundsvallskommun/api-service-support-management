package se.sundsvall.supportmanagement.service.mapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import se.sundsvall.supportmanagement.api.model.subscriber.EventFilter;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionProfile;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.EventFilterEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionProfileEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SubscriptionProfileMapperTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-10-01T08:00:00+02:00");
	private static final OffsetDateTime MODIFIED = OffsetDateTime.parse("2026-10-02T08:00:00+02:00");

	@ParameterizedTest
	@MethodSource("toSubscriptionProfileEntityArguments")
	void toSubscriptionProfileEntity(final SubscriptionProfile input, final SubscriptionProfileEntity expected) {
		final var result = SubscriptionProfileMapper.toSubscriptionProfileEntity(MUNICIPALITY_ID, NAMESPACE, input);

		if (expected == null) {
			assertThat(result).isNull();
		} else {
			assertThat(result).usingRecursiveComparison().isEqualTo(expected);
		}
	}

	private static Stream<Arguments> toSubscriptionProfileEntityArguments() {
		return Stream.of(
			Arguments.of(null, null),
			Arguments.of(
				SubscriptionProfile.create()
					.withName("Mejl")
					.withDescription("Mejl om nya ärenden")
					.withEventFilters(List.of(EventFilter.create().withType("CREATE").withSubtype("ERRAND")))
					.withChannels(List.of(NotificationChannelType.EMAIL)),
				SubscriptionProfileEntity.create()
					.withMunicipalityId(MUNICIPALITY_ID)
					.withNamespace(NAMESPACE)
					.withName("Mejl")
					.withDescription("Mejl om nya ärenden")
					.withEventFilters(List.of(EventFilterEmbeddable.create().withType("CREATE").withSubtype("ERRAND")))
					.withChannels(List.of(se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.EMAIL))),
			Arguments.of(
				SubscriptionProfile.create().withName("Tom"),
				SubscriptionProfileEntity.create()
					.withMunicipalityId(MUNICIPALITY_ID)
					.withNamespace(NAMESPACE)
					.withName("Tom")));
	}

	@ParameterizedTest
	@MethodSource("toSubscriptionProfileArguments")
	void toSubscriptionProfile(final SubscriptionProfileEntity input, final SubscriptionProfile expected) {
		final var result = SubscriptionProfileMapper.toSubscriptionProfile(input);

		if (expected == null) {
			assertThat(result).isNull();
		} else {
			assertThat(result).usingRecursiveComparison().isEqualTo(expected);
		}
	}

	private static Stream<Arguments> toSubscriptionProfileArguments() {
		return Stream.of(
			Arguments.of(null, null),
			Arguments.of(
				SubscriptionProfileEntity.create()
					.withId("profile-1")
					.withMunicipalityId(MUNICIPALITY_ID)
					.withNamespace(NAMESPACE)
					.withName("Notis")
					.withDescription("Notis om meddelanden")
					.withEventFilters(List.of(EventFilterEmbeddable.create().withType("UPDATE").withSubtype("MESSAGE")))
					.withChannels(List.of(se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.INTERNAL))
					.withCreated(CREATED)
					.withModified(MODIFIED),
				SubscriptionProfile.create()
					.withId("profile-1")
					.withName("Notis")
					.withDescription("Notis om meddelanden")
					.withEventFilters(List.of(EventFilter.create().withType("UPDATE").withSubtype("MESSAGE")))
					.withChannels(List.of(NotificationChannelType.INTERNAL))
					.withCreated(CREATED)
					.withModified(MODIFIED)));
	}

	@Test
	void toSubscriptionProfileList() {
		final var entities = List.of(
			SubscriptionProfileEntity.create().withId("1").withName("a"),
			SubscriptionProfileEntity.create().withId("2").withName("b"));

		final var result = SubscriptionProfileMapper.toSubscriptionProfileList(entities);

		assertThat(result)
			.extracting(SubscriptionProfile::getId, SubscriptionProfile::getName)
			.containsExactly(tuple("1", "a"), tuple("2", "b"));
	}

	@Test
	void toSubscriptionProfileListWithNull() {
		assertThat(SubscriptionProfileMapper.toSubscriptionProfileList(null)).isEmpty();
	}

	@Test
	void updateEntityAppliesOnlyNonNullFields() {
		final var entity = SubscriptionProfileEntity.create()
			.withName("Gammalt namn")
			.withDescription("Gammal beskrivning")
			.withEventFilters(List.of(EventFilterEmbeddable.create().withType("CREATE")))
			.withChannels(List.of(se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.EMAIL));

		SubscriptionProfileMapper.updateEntity(entity, SubscriptionProfile.create()
			.withName("Nytt namn")
			.withChannels(List.of(NotificationChannelType.INTERNAL, NotificationChannelType.EMAIL)));

		assertThat(entity.getName()).isEqualTo("Nytt namn");
		assertThat(entity.getDescription()).isEqualTo("Gammal beskrivning");
		assertThat(entity.getEventFilters()).containsExactly(EventFilterEmbeddable.create().withType("CREATE"));
		assertThat(entity.getChannels()).containsExactly(
			se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.INTERNAL,
			se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.EMAIL);
	}

	@Test
	void updateEntityReplacesEventFiltersAndDescription() {
		final var entity = SubscriptionProfileEntity.create().withName("Namn");

		SubscriptionProfileMapper.updateEntity(entity, SubscriptionProfile.create()
			.withDescription("Ny beskrivning")
			.withEventFilters(List.of(EventFilter.create().withType("UPDATE").withSubtype("MESSAGE"))));

		assertThat(entity.getName()).isEqualTo("Namn");
		assertThat(entity.getDescription()).isEqualTo("Ny beskrivning");
		assertThat(entity.getEventFilters()).containsExactly(EventFilterEmbeddable.create().withType("UPDATE").withSubtype("MESSAGE"));
	}

	@Test
	void updateEntityWithNullsDoesNothing() {
		final var entity = SubscriptionProfileEntity.create().withName("Namn");

		SubscriptionProfileMapper.updateEntity(entity, null);
		SubscriptionProfileMapper.updateEntity(null, SubscriptionProfile.create());

		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("name");
	}
}

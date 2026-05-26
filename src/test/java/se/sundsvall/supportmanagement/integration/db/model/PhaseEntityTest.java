package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class PhaseEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(PhaseEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("transitions"),
			hasValidBeanEqualsExcluding("transitions"),
			hasValidBeanToStringExcluding("transitions")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "id";
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var name = "name";
		final var displayName = "displayName";
		final var description = "description";
		final var phaseOrder = 1;
		final var allowedStatuses = List.of("IN_PROGRESS", "WAITING");
		final var transitions = List.of(PhaseTransitionEntity.create());
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var deprecated = true;

		final var entity = PhaseEntity.create()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withName(name)
			.withDisplayName(displayName)
			.withDescription(description)
			.withPhaseOrder(phaseOrder)
			.withDeprecated(deprecated)
			.withAllowedStatuses(allowedStatuses)
			.withTransitions(transitions)
			.withCreated(created)
			.withModified(modified);

		assertThat(entity).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getName()).isEqualTo(name);
		assertThat(entity.getDisplayName()).isEqualTo(displayName);
		assertThat(entity.getDescription()).isEqualTo(description);
		assertThat(entity.getPhaseOrder()).isEqualTo(phaseOrder);
		assertThat(entity.isDeprecated()).isEqualTo(deprecated);
		assertThat(entity.getAllowedStatuses()).isEqualTo(allowedStatuses);
		assertThat(entity.getTransitions()).isEqualTo(transitions);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void testOnCreate() {
		final var entity = PhaseEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("allowedStatuses", "transitions", "created", "deprecated")
			.satisfies(e -> {
				assertThat(e.getAllowedStatuses()).isEmpty();
				assertThat(e.getTransitions()).isEmpty();
			});
	}

	@Test
	void testOnUpdate() {
		final var entity = PhaseEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("allowedStatuses", "transitions", "modified", "deprecated")
			.satisfies(e -> {
				assertThat(e.getAllowedStatuses()).isEmpty();
				assertThat(e.getTransitions()).isEmpty();
			});
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PhaseEntity.create()).hasAllNullFieldsOrPropertiesExcept("allowedStatuses", "transitions", "deprecated")
			.satisfies(e -> {
				assertThat(e.getAllowedStatuses()).isEmpty();
				assertThat(e.getTransitions()).isEmpty();
			});
		assertThat(new PhaseEntity()).hasAllNullFieldsOrPropertiesExcept("allowedStatuses", "transitions", "deprecated")
			.satisfies(e -> {
				assertThat(e.getAllowedStatuses()).isEmpty();
				assertThat(e.getTransitions()).isEmpty();
			});
	}
}

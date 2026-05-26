package se.sundsvall.supportmanagement.integration.db.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class PhaseTransitionEntityTest {

	@Test
	void testBean() {
		assertThat(PhaseTransitionEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("phaseEntity"),
			hasValidBeanEqualsExcluding("phaseEntity"),
			hasValidBeanToStringExcluding("phaseEntity")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = "id";
		final var phaseEntity = PhaseEntity.create().withId("phase-id");
		final var targetPhaseId = "target-phase-id";
		final var description = "description";
		final var deprecated = true;

		final var entity = PhaseTransitionEntity.create()
			.withId(id)
			.withPhaseEntity(phaseEntity)
			.withTargetPhaseId(targetPhaseId)
			.withDescription(description)
			.withDeprecated(deprecated);

		assertThat(entity).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getPhaseEntity()).isEqualTo(phaseEntity);
		assertThat(entity.getTargetPhaseId()).isEqualTo(targetPhaseId);
		assertThat(entity.getDescription()).isEqualTo(description);
		assertThat(entity.isDeprecated()).isEqualTo(deprecated);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PhaseTransitionEntity.create()).hasAllNullFieldsOrPropertiesExcept("deprecated");
		assertThat(new PhaseTransitionEntity()).hasAllNullFieldsOrPropertiesExcept("deprecated");
	}
}

package se.sundsvall.supportmanagement.api.model.metadata;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class PhaseTransitionTest {

	@Test
	void testBean() {
		assertThat(PhaseTransition.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {

		final var id = "id";
		final var targetPhaseId = "targetPhaseId";
		final var targetPhaseName = "targetPhaseName";
		final var targetPhaseDisplayName = "targetPhaseDisplayName";
		final var deprecated = true;
		final var description = "description";

		final var bean = PhaseTransition.create()
			.withId(id)
			.withTargetPhaseId(targetPhaseId)
			.withTargetPhaseName(targetPhaseName)
			.withTargetPhaseDisplayName(targetPhaseDisplayName)
			.withDeprecated(deprecated)
			.withDescription(description);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getTargetPhaseId()).isEqualTo(targetPhaseId);
		assertThat(bean.getTargetPhaseName()).isEqualTo(targetPhaseName);
		assertThat(bean.getTargetPhaseDisplayName()).isEqualTo(targetPhaseDisplayName);
		assertThat(bean.getDeprecated()).isEqualTo(deprecated);
		assertThat(bean.getDescription()).isEqualTo(description);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PhaseTransition.create()).hasAllNullFieldsOrProperties();
		assertThat(new PhaseTransition()).hasAllNullFieldsOrProperties();
	}
}

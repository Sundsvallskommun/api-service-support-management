package se.sundsvall.supportmanagement.api.model.errand.handover;

import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.Priority;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class HandoverOverridesTest {

	@Test
	void testBean() {
		assertThat(HandoverOverrides.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var title = "Override title";
		final var description = "Override description";
		final var priority = Priority.MEDIUM;
		final var assignedUserId = "user-uuid";
		final var assignedGroupId = "group-uuid";

		final var overrides = HandoverOverrides.create()
			.withTitle(title)
			.withDescription(description)
			.withPriority(priority)
			.withAssignedUserId(assignedUserId)
			.withAssignedGroupId(assignedGroupId);

		assertThat(overrides).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(overrides.getTitle()).isEqualTo(title);
		assertThat(overrides.getDescription()).isEqualTo(description);
		assertThat(overrides.getPriority()).isEqualTo(priority);
		assertThat(overrides.getAssignedUserId()).isEqualTo(assignedUserId);
		assertThat(overrides.getAssignedGroupId()).isEqualTo(assignedGroupId);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(HandoverOverrides.create()).hasAllNullFieldsOrProperties();
		assertThat(new HandoverOverrides()).hasAllNullFieldsOrProperties();
	}
}

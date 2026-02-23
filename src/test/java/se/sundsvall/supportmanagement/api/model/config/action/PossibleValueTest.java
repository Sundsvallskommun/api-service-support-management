package se.sundsvall.supportmanagement.api.model.config.action;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class PossibleValueTest {

	@Test
	void testBean() {
		assertThat(PossibleValue.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var value = "value";
		final var displayName = "displayName";

		final var bean = PossibleValue.create()
			.withValue(value)
			.withDisplayName(displayName);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getValue()).isEqualTo(value);
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PossibleValue.create()).hasAllNullFieldsOrProperties();
		assertThat(new PossibleValue()).hasAllNullFieldsOrProperties();
	}
}

package se.sundsvall.supportmanagement.api.model.handover;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class NotCopyableTest {

	@Test
	void testBean() {
		assertThat(NotCopyable.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var field = "phases";
		final var reason = "Phase history is source-specific";

		final var bean = NotCopyable.create()
			.withField(field)
			.withReason(reason);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getField()).isEqualTo(field);
		assertThat(bean.getReason()).isEqualTo(reason);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NotCopyable.create()).hasAllNullFieldsOrProperties();
		assertThat(new NotCopyable()).hasAllNullFieldsOrProperties();
	}
}

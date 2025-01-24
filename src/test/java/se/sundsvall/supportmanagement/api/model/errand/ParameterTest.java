package se.sundsvall.supportmanagement.api.model.errand;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ParameterTest {

	@Test
	void testBean() {
		assertThat(Parameter.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var displayName = "displayName";
		final var key = "key";
		final var values = List.of("value");
		final var group = "group";

		final var bean = Parameter.create()
			.withDisplayName(displayName)
			.withKey(key)
			.withValues(values)
			.withGroup(group);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
		assertThat(bean.getKey()).isEqualTo(key);
		assertThat(bean.getValues()).isEqualTo(values);
		assertThat(bean.getGroup()).isEqualTo(group);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Parameter.create()).hasAllNullFieldsOrProperties();
		assertThat(new Parameter()).hasAllNullFieldsOrProperties();
	}

}

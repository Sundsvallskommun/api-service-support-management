package se.sundsvall.supportmanagement.api.model.config;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class AccessResourceDefinitionTest {

	@Test
	void testBean() {
		assertThat(AccessResourceDefinition.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = AccessResourceDefinition.create()
			.withResource("COMMUNICATION")
			.withPath("errand/communication")
			.withErrandScoped(true);

		org.assertj.core.api.Assertions.assertThat(bean).hasNoNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(bean.getResource()).isEqualTo("COMMUNICATION");
		org.assertj.core.api.Assertions.assertThat(bean.getPath()).isEqualTo("errand/communication");
		org.assertj.core.api.Assertions.assertThat(bean.isErrandScoped()).isTrue();
	}
}

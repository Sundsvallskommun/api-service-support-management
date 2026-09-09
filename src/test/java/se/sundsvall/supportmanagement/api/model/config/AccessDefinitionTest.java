package se.sundsvall.supportmanagement.api.model.config;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class AccessDefinitionTest {

	@Test
	void testBean() {
		assertThat(AccessDefinition.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var fields = java.util.List.of(AccessFieldDefinition.create().withField("TITLE"));
		final var resources = java.util.List.of(AccessResourceDefinition.create().withResource("ERRAND"));
		final var bean = AccessDefinition.create()
			.withFields(fields)
			.withResources(resources);

		org.assertj.core.api.Assertions.assertThat(bean).hasNoNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(bean.getFields()).isEqualTo(fields);
		org.assertj.core.api.Assertions.assertThat(bean.getResources()).isEqualTo(resources);
	}
}

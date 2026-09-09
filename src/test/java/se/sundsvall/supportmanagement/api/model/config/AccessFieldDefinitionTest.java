package se.sundsvall.supportmanagement.api.model.config;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class AccessFieldDefinitionTest {

	@Test
	void testBean() {
		assertThat(AccessFieldDefinition.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = AccessFieldDefinition.create()
			.withField("PARAMETERS")
			.withProperty("parameters")
			.withKeyed(true);

		org.assertj.core.api.Assertions.assertThat(bean).hasNoNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(bean.getField()).isEqualTo("PARAMETERS");
		org.assertj.core.api.Assertions.assertThat(bean.getProperty()).isEqualTo("parameters");
		org.assertj.core.api.Assertions.assertThat(bean.isKeyed()).isTrue();
	}
}

package se.sundsvall.supportmanagement.api.model.revision;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class OperationTest {

	@Test
	void testBean() {
		assertThat(Operation.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var bean = Operation.create()
			.withOp("op")
			.withPath("path")
			.withValue("value")
			.withFromValue("fromValue");

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getOp()).isEqualTo("op");
		assertThat(bean.getPath()).isEqualTo("path");
		assertThat(bean.getValue()).isEqualTo("value");
		assertThat(bean.getFromValue()).isEqualTo("fromValue");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Operation.create()).hasAllNullFieldsOrProperties();
		assertThat(new Operation()).hasAllNullFieldsOrProperties();
	}
}

package se.sundsvall.supportmanagement.api.model.revision;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class DifferenceResponseTest {

	@Test
	void testBean() {
		assertThat(DifferenceResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var operations = List.of(Operation.create().withOp("op").withPath("path").withValue("value").withFromValue("fromValue"));

		final var bean = DifferenceResponse.create()
				.withOperations(operations);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getOperations()).isEqualTo(operations);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DifferenceResponse.create()).hasAllNullFieldsOrProperties();
		assertThat(new DifferenceResponse()).hasAllNullFieldsOrProperties();
	}
}

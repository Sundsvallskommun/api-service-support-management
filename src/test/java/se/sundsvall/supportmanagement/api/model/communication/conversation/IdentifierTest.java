package se.sundsvall.supportmanagement.api.model.communication.conversation;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class IdentifierTest {

	@Test
	void testBean() {
		assertThat(Identifier.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var type = "type";
		final var value = "value";

		final var o = Identifier.create()
			.withType(type)
			.withValue(value);

		assertThat(o).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(o.getType()).isEqualTo(type);
		assertThat(o.getValue()).isEqualTo(value);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Identifier.create()).hasAllNullFieldsOrProperties();
		assertThat(new Identifier()).hasAllNullFieldsOrProperties();
	}
}

package se.sundsvall.supportmanagement.api.model.identifier;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class IdentifierTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(Identifier.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var type = "adAccount";
		final var value = "joe01doe";

		final var identifier = Identifier.create()
			.withType(type)
			.withValue(value);

		assertThat(identifier.getType()).isEqualTo(type);
		assertThat(identifier.getValue()).isEqualTo(value);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Identifier.create()).hasAllNullFieldsOrProperties();
		assertThat(new Identifier()).hasAllNullFieldsOrProperties();
	}
}

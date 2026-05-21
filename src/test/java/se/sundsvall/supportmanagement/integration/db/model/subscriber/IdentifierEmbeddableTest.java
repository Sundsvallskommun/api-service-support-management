package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class IdentifierEmbeddableTest {

	@Test
	void testBean() {
		assertThat(IdentifierEmbeddable.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var type = "adAccount";
		final var value = "joe01doe";

		final var identifier = IdentifierEmbeddable.create()
			.withType(type)
			.withValue(value);

		assertThat(identifier).hasNoNullFieldsOrProperties();
		assertThat(identifier.getType()).isEqualTo(type);
		assertThat(identifier.getValue()).isEqualTo(value);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(IdentifierEmbeddable.create()).hasAllNullFieldsOrProperties();
		assertThat(new IdentifierEmbeddable()).hasAllNullFieldsOrProperties();
	}
}

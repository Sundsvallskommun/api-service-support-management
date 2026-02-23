package se.sundsvall.supportmanagement.api.model.config.action;

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

class DefinitionTest {

	@Test
	void testBean() {
		assertThat(Definition.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testMandatoryDefaultValue() {
		assertThat(new Definition().getMandatory()).isFalse();
	}

	@Test
	void testBuilderMethods() {
		final var key = "key";
		final var mandatory = true;
		final var description = "description";
		final var possibleValues = List.of(PossibleValue.create().withValue("val"));

		final var bean = Definition.create()
			.withKey(key)
			.withMandatory(mandatory)
			.withDescription(description)
			.withPossibleValues(possibleValues);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getKey()).isEqualTo(key);
		assertThat(bean.getMandatory()).isEqualTo(mandatory);
		assertThat(bean.getDescription()).isEqualTo(description);
		assertThat(bean.getPossibleValues()).isEqualTo(possibleValues);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Definition.create()).hasAllNullFieldsOrPropertiesExcept("mandatory");
		assertThat(new Definition()).hasAllNullFieldsOrPropertiesExcept("mandatory");
	}
}

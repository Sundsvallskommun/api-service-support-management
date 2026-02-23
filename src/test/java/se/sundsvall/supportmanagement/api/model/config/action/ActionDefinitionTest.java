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

class ActionDefinitionTest {

	@Test
	void testBean() {
		assertThat(ActionDefinition.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var name = "name";
		final var description = "description";
		final var conditionDefinitions = List.of(Definition.create().withKey("condKey"));
		final var parameterDefinitions = List.of(Definition.create().withKey("paramKey"));

		final var bean = ActionDefinition.create()
			.withName(name)
			.withDescription(description)
			.withConditionDefinitions(conditionDefinitions)
			.withParameterDefinitions(parameterDefinitions);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getName()).isEqualTo(name);
		assertThat(bean.getDescription()).isEqualTo(description);
		assertThat(bean.getConditionDefinitions()).isEqualTo(conditionDefinitions);
		assertThat(bean.getParameterDefinitions()).isEqualTo(parameterDefinitions);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ActionDefinition.create()).hasAllNullFieldsOrProperties();
		assertThat(new ActionDefinition()).hasAllNullFieldsOrProperties();
	}
}

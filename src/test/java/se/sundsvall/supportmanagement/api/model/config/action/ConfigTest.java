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

class ConfigTest {

	@Test
	void testBean() {
		assertThat(Config.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "id";
		final var name = "name";
		final var active = true;
		final var conditions = List.of(Parameter.create().withKey("conditionKey"));
		final var parameters = List.of(Parameter.create().withKey("parameterKey"));
		final var displayValue = "displayValue";

		final var bean = Config.create()
			.withId(id)
			.withName(name)
			.withActive(active)
			.withConditions(conditions)
			.withParameters(parameters)
			.withDisplayValue(displayValue);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getName()).isEqualTo(name);
		assertThat(bean.getActive()).isEqualTo(active);
		assertThat(bean.getConditions()).isEqualTo(conditions);
		assertThat(bean.getParameters()).isEqualTo(parameters);
		assertThat(bean.getDisplayValue()).isEqualTo(displayValue);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Config.create()).hasAllNullFieldsOrProperties();
		assertThat(new Config()).hasAllNullFieldsOrProperties();
	}
}

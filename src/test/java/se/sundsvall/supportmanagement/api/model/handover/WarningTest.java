package se.sundsvall.supportmanagement.api.model.handover;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class WarningTest {

	@Test
	void testBean() {
		assertThat(Warning.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var type = WarningType.PARAMETER_SCHEMA_MISMATCH;
		final var key = "orgUnit";
		final var detail = "jsonSchema 'orgUnit-v2' not registered in target";
		final var value = "EXTERNAL_REPORTER";

		final var bean = Warning.create()
			.withType(type)
			.withKey(key)
			.withDetail(detail)
			.withValue(value);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getType()).isEqualTo(type);
		assertThat(bean.getKey()).isEqualTo(key);
		assertThat(bean.getDetail()).isEqualTo(detail);
		assertThat(bean.getValue()).isEqualTo(value);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Warning.create()).hasAllNullFieldsOrProperties();
		assertThat(new Warning()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testParameterSchemaMismatchFactory() {
		final var key = "orgUnit";
		final var detail = "jsonSchema 'orgUnit-v2' not registered in target";

		final var bean = Warning.parameterSchemaMismatch(key, detail);

		assertThat(bean.getType()).isEqualTo(WarningType.PARAMETER_SCHEMA_MISMATCH);
		assertThat(bean.getKey()).isEqualTo(key);
		assertThat(bean.getDetail()).isEqualTo(detail);
		assertThat(bean.getValue()).isNull();
	}

	@Test
	void testRoleNotInTargetFactory() {
		final var value = "EXTERNAL_REPORTER";

		final var bean = Warning.roleNotInTarget(value);

		assertThat(bean.getType()).isEqualTo(WarningType.ROLE_NOT_IN_TARGET);
		assertThat(bean.getValue()).isEqualTo(value);
		assertThat(bean.getKey()).isNull();
		assertThat(bean.getDetail()).isNull();
	}
}

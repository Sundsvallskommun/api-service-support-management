package se.sundsvall.supportmanagement.api.model.config;

import java.util.List;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField.PARAMETERS;

class FieldAccessTest {

	@Test
	void testBean() {
		assertThat(FieldAccess.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var keys = List.of("key-1");

		final var bean = FieldAccess.create()
			.withField(PARAMETERS)
			.withKeys(keys);

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getField()).isEqualTo(PARAMETERS);
		assertThat(bean.getKeys()).isEqualTo(keys);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FieldAccess.create()).hasAllNullFieldsOrProperties();
		assertThat(new FieldAccess()).hasAllNullFieldsOrProperties();
	}

}

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
import static se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField.TITLE;

class RoleAccessTest {

	@Test
	void testBean() {
		assertThat(RoleFieldRestriction.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var fields = List.of(FieldAccess.create().withField(TITLE));

		final var bean = RoleFieldRestriction.create()
			.withRole("FIRST_LINE_CASE_OFFICER")
			.withFields(fields);

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getRole()).isEqualTo("FIRST_LINE_CASE_OFFICER");
		assertThat(bean.getFields()).isEqualTo(fields);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(RoleFieldRestriction.create()).hasAllNullFieldsOrProperties();
		assertThat(new RoleFieldRestriction()).hasAllNullFieldsOrProperties();
	}
}

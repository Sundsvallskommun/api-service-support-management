package se.sundsvall.supportmanagement.api.model.access;

import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ErrandFieldKeyAccessTest {

	@Test
	void testBean() {
		assertThat(ErrandFieldKeyAccess.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = ErrandFieldKeyAccess.create()
			.withKey("contactChannel")
			.withLevel(AccessLevel.RW);

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getKey()).isEqualTo("contactChannel");
		assertThat(bean.getLevel()).isEqualTo(AccessLevel.RW);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandFieldKeyAccess.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandFieldKeyAccess()).hasAllNullFieldsOrProperties();
	}
}

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

class ErrandResourceAccessTest {

	@Test
	void testBean() {
		assertThat(ErrandResourceAccess.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = ErrandResourceAccess.create()
			.withResource("errand/communication")
			.withLevel(AccessLevel.R);

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getResource()).isEqualTo("errand/communication");
		assertThat(bean.getLevel()).isEqualTo(AccessLevel.R);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandResourceAccess.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandResourceAccess()).hasAllNullFieldsOrProperties();
	}
}

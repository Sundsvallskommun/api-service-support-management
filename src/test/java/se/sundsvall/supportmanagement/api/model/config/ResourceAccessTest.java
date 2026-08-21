package se.sundsvall.supportmanagement.api.model.config;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource.COMMUNICATION;

class ResourceAccessTest {

	@Test
	void testBean() {
		assertThat(ResourceAccess.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var bean = ResourceAccess.create()
			.withResource(COMMUNICATION)
			.withLevel(AccessLevel.R);

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getResource()).isEqualTo(COMMUNICATION);
		assertThat(bean.getLevel()).isEqualTo(AccessLevel.R);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ResourceAccess.create()).hasAllNullFieldsOrProperties();
		assertThat(new ResourceAccess()).hasAllNullFieldsOrProperties();
	}
}

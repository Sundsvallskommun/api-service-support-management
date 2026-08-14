package se.sundsvall.supportmanagement.integration.db.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static se.sundsvall.supportmanagement.integration.db.model.enums.RoleAccessType.RESOURCE;

class NamespaceConfigAccessGrantEmbeddableTest {

	@Test
	void testBean() {
		assertThat(NamespaceConfigAccessGrantEmbeddable.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var bean = NamespaceConfigAccessGrantEmbeddable.create()
			.withScope("REPORTER")
			.withType(RESOURCE)
			.withValue("ERRAND")
			.withAccessLevel("R");

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getScope()).isEqualTo("REPORTER");
		assertThat(bean.getType()).isEqualTo(RESOURCE);
		assertThat(bean.getValue()).isEqualTo("ERRAND");
		assertThat(bean.getAccessLevel()).isEqualTo("R");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NamespaceConfigAccessGrantEmbeddable.create()).hasAllNullFieldsOrProperties();
		assertThat(new NamespaceConfigAccessGrantEmbeddable()).hasAllNullFieldsOrProperties();
	}
}

package se.sundsvall.supportmanagement.integration.db.model;

import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.ValueType;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class NamespaceConfigValueEmbeddableTest {

	@Test
	void testBean() {
		assertThat(NamespaceConfigValueEmbeddable.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var key = "key";
		final var value = "value";
		final var type = ValueType.STRING;
		final var bean = NamespaceConfigValueEmbeddable.create()
			.withKey(key)
			.withType(type)
			.withValue(value);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getKey()).isEqualTo(key);
		assertThat(bean.getType()).isEqualTo(type);
		assertThat(bean.getValue()).isEqualTo(value);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NamespaceConfigValueEmbeddable.create()).hasAllNullFieldsOrProperties();
		assertThat(new NamespaceConfigValueEmbeddable()).hasAllNullFieldsOrProperties();
	}
}

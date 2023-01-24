package se.sundsvall.supportmanagement.integration.db.model;

import org.junit.jupiter.api.Test;

import se.sundsvall.supportmanagement.api.model.errand.CustomerType;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class EmbeddableCustomerTest {

	@Test
	void testBean() {
		assertThat(EmbeddableCustomer.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "id";
		final var customerType = CustomerType.EMPLOYEE.toString();

		final var bean = EmbeddableCustomer.create()
			.withId(id)
			.withType(customerType);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getType()).isEqualTo(customerType);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(EmbeddableCustomer.create()).hasAllNullFieldsOrProperties();
	}
}

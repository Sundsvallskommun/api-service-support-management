package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import org.junit.jupiter.api.Test;

class ContactChannelEntityTest {

	@Test
	void testBean() {
		assertThat(ContactChannelEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var type = "type";
		final var value = "value";

		final var contactChannelEntity = ContactChannelEntity.create()
			.withType(type)
			.withValue(value);

		assertThat(contactChannelEntity.getType()).isEqualTo(type);
		assertThat(contactChannelEntity.getValue()).isEqualTo(value);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(ContactChannelEntity.create()).hasAllNullFieldsOrPropertiesExcept("id");
		assertThat(new ContactChannelEntity()).hasAllNullFieldsOrPropertiesExcept("id");
	}
}

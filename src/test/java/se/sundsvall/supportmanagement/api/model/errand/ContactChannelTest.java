package se.sundsvall.supportmanagement.api.model.errand;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.*;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

class ContactChannelTest {
	@Test
	void testBean() {
		assertThat(ContactChannel.class, allOf(
				hasValidBeanConstructor(),
				hasValidGettersAndSetters(),
				hasValidBeanHashCode(),
				hasValidBeanEquals(),
				hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		var type = "type";
		var value = "value";

		var bean = ContactChannel.create()
				.withType(type)
				.withValue(value);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getType()).isEqualTo(type);
		assertThat(bean.getValue()).isEqualTo(value);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ContactChannel.create()).hasAllNullFieldsOrProperties();
	}
}
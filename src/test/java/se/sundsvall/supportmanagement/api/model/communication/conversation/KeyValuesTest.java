package se.sundsvall.supportmanagement.api.model.communication.conversation;

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

class KeyValuesTest {

	@Test
	void testBean() {
		assertThat(KeyValues.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var key = "key";
		final var values = List.of("value");

		final var object = KeyValues.create()
			.withKey(key)
			.withValues(values);

		assertThat(object).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(object.getKey()).isEqualTo(key);
		assertThat(object.getValues()).isEqualTo(values);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(KeyValues.create()).hasAllNullFieldsOrProperties();
		assertThat(new KeyValues()).hasAllNullFieldsOrProperties();
	}
}

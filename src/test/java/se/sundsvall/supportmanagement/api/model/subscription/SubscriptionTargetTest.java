package se.sundsvall.supportmanagement.api.model.subscription;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class SubscriptionTargetTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(SubscriptionTarget.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var type = SubscriptionTargetType.ERRAND;
		final var id = "b82bd8ac-1507-4d9a-958d-369261eecc15";

		final var target = SubscriptionTarget.create()
			.withType(type)
			.withId(id);

		assertThat(target.getType()).isEqualTo(type);
		assertThat(target.getId()).isEqualTo(id);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(SubscriptionTarget.create()).hasAllNullFieldsOrProperties();
		assertThat(new SubscriptionTarget()).hasAllNullFieldsOrProperties();
	}
}

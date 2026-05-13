package se.sundsvall.supportmanagement.api.model.subscriber;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class NotificationChannelTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(NotificationChannel.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var type = NotificationChannelType.EMAIL;
		final var destination = "user@example.com";

		final var channel = NotificationChannel.create()
			.withType(type)
			.withDestination(destination);

		assertThat(channel.getType()).isEqualTo(type);
		assertThat(channel.getDestination()).isEqualTo(destination);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NotificationChannel.create()).hasAllNullFieldsOrProperties();
		assertThat(new NotificationChannel()).hasAllNullFieldsOrProperties();
	}
}

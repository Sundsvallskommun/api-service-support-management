package se.sundsvall.supportmanagement.api.model.notification;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class NotificationGroupTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(NotificationGroup.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var requestGroupId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
		final var notifications = of(Notification.create());

		final var group = NotificationGroup.create()
			.withRequestGroupId(requestGroupId)
			.withNotifications(notifications);

		assertThat(group.getRequestGroupId()).isEqualTo(requestGroupId);
		assertThat(group.getNotifications()).isEqualTo(notifications);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NotificationGroup.create()).hasAllNullFieldsOrProperties();
	}
}

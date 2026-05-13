package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class NotificationChannelEmbeddableTest {

	@Test
	void testBean() {
		assertThat(NotificationChannelEmbeddable.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var type = NotificationChannelType.EMAIL;
		final var destination = "user@example.com";

		final var channel = NotificationChannelEmbeddable.create()
			.withType(type)
			.withDestination(destination);

		assertThat(channel).hasNoNullFieldsOrProperties();
		assertThat(channel.getType()).isEqualTo(type);
		assertThat(channel.getDestination()).isEqualTo(destination);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(NotificationChannelEmbeddable.create()).hasAllNullFieldsOrProperties();
		assertThat(new NotificationChannelEmbeddable()).hasAllNullFieldsOrProperties();
	}
}

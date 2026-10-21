package se.sundsvall.supportmanagement.integration.db.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class EmailDispatchOutboxEventEmbeddableTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(EmailDispatchOutboxEventEmbeddable.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var eventId = "event-id";
		final var description = "En anteckning har skapats";

		final var bean = EmailDispatchOutboxEventEmbeddable.create()
			.withEventId(eventId)
			.withDescription(description);

		assertThat(bean.getEventId()).isEqualTo(eventId);
		assertThat(bean.getDescription()).isEqualTo(description);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(EmailDispatchOutboxEventEmbeddable.create()).hasAllNullFieldsOrProperties();
		assertThat(new EmailDispatchOutboxEventEmbeddable()).hasAllNullFieldsOrProperties();
	}
}

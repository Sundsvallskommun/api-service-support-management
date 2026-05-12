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

class EventFilterTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(EventFilter.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var type = "UPDATE";
		final var subtype = "ATTACHMENT";

		final var filter = EventFilter.create()
			.withType(type)
			.withSubtype(subtype);

		assertThat(filter.getType()).isEqualTo(type);
		assertThat(filter.getSubtype()).isEqualTo(subtype);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(EventFilter.create()).hasAllNullFieldsOrProperties();
		assertThat(new EventFilter()).hasAllNullFieldsOrProperties();
	}
}

package se.sundsvall.supportmanagement.integration.db.model.subscriber;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class EventFilterEmbeddableTest {

	@Test
	void testBean() {
		assertThat(EventFilterEmbeddable.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var type = "UPDATE";
		final var subtype = "ATTACHMENT";
		final var labelAdded = "f2b7c5d1-7e3a-4b8e-9f0a-1c2d3e4f5a6b";

		final var filter = EventFilterEmbeddable.create()
			.withType(type)
			.withSubtype(subtype)
			.withLabelAdded(labelAdded);

		assertThat(filter).hasNoNullFieldsOrProperties();
		assertThat(filter.getType()).isEqualTo(type);
		assertThat(filter.getSubtype()).isEqualTo(subtype);
		assertThat(filter.getLabelAdded()).isEqualTo(labelAdded);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(EventFilterEmbeddable.create()).hasAllNullFieldsOrProperties();
		assertThat(new EventFilterEmbeddable()).hasAllNullFieldsOrProperties();
	}
}

package se.sundsvall.supportmanagement.api.model.communication.conversation;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class PartReadByCountEntryTest {

	@Test
	void testBean() {
		assertThat(PartReadByCountEntry.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var part = "KC-23010001";
		final var count = 3;

		final var o = PartReadByCountEntry.create()
			.withPart(part)
			.withCount(count);

		assertThat(o).isNotNull();
		assertThat(o.getPart()).isEqualTo(part);
		assertThat(o.getCount()).isEqualTo(count);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PartReadByCountEntry.create()).hasAllNullFieldsOrProperties();
		assertThat(new PartReadByCountEntry()).hasAllNullFieldsOrProperties();
	}
}

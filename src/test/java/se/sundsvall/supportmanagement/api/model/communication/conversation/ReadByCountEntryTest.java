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

class ReadByCountEntryTest {

	@Test
	void testBean() {
		assertThat(ReadByCountEntry.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var identifier = Identifier.create().withType("adAccount").withValue("joe01doe");
		final var count = 5;

		final var o = ReadByCountEntry.create()
			.withIdentifier(identifier)
			.withCount(count);

		assertThat(o).isNotNull();
		assertThat(o.getIdentifier()).isEqualTo(identifier);
		assertThat(o.getCount()).isEqualTo(count);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ReadByCountEntry.create()).hasAllNullFieldsOrProperties();
		assertThat(new ReadByCountEntry()).hasAllNullFieldsOrProperties();
	}
}

package se.sundsvall.supportmanagement.api.model.handover;

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

class ContactReasonMappingTest {

	@Test
	void testBean() {
		assertThat(ContactReasonMapping.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var source = "Bygglov";
		final var suggested = "Bygglov";
		final var candidates = List.of("Bygglov", "Felanmälan");

		final var bean = ContactReasonMapping.create()
			.withSource(source)
			.withSuggested(suggested)
			.withCandidates(candidates);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getSource()).isEqualTo(source);
		assertThat(bean.getSuggested()).isEqualTo(suggested);
		assertThat(bean.getCandidates()).isEqualTo(candidates);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ContactReasonMapping.create()).hasAllNullFieldsOrProperties();
		assertThat(new ContactReasonMapping()).hasAllNullFieldsOrProperties();
	}
}

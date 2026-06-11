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

class SourceHandlingTest {

	@Test
	void testBean() {
		assertThat(SourceHandling.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var statusCandidates = List.of(MetadataOption.create().withName("SOLVED").withDisplayName("Löst"));

		final var bean = SourceHandling.create()
			.withStatusCandidates(statusCandidates);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getStatusCandidates()).isEqualTo(statusCandidates);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(SourceHandling.create()).hasAllNullFieldsOrProperties();
		assertThat(new SourceHandling()).hasAllNullFieldsOrProperties();
	}
}

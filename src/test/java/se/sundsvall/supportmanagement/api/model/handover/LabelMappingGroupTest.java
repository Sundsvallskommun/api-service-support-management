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

class LabelMappingGroupTest {

	@Test
	void testBean() {
		assertThat(LabelMappingGroup.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var candidates = List.of(LabelCandidate.create().withId("uuid-b").withDisplayName("Nyckelkort").withResourcePath("/access/keycard"));
		final var mappings = List.of(LabelMapping.create().withSourceId("uuid-a"));

		final var bean = LabelMappingGroup.create()
			.withCandidates(candidates)
			.withMappings(mappings);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getCandidates()).isEqualTo(candidates);
		assertThat(bean.getMappings()).isEqualTo(mappings);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LabelMappingGroup.create()).hasAllNullFieldsOrProperties();
		assertThat(new LabelMappingGroup()).hasAllNullFieldsOrProperties();
	}
}

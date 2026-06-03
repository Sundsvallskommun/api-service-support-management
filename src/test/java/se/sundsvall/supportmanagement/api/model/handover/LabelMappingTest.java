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

class LabelMappingTest {

	@Test
	void testBean() {
		assertThat(LabelMapping.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var sourceId = "uuid-a";
		final var sourceDisplayName = "Nyckelkort";
		final var sourceResourcePath = "/access/keycard";
		final var suggestedTargetId = "uuid-b";
		final var matchReason = MatchReason.RESOURCE_PATH_MATCH;
		final var candidates = List.of(LabelCandidate.create().withId("uuid-b").withDisplayName("Nyckelkort").withResourcePath("/access/keycard"));

		final var bean = LabelMapping.create()
			.withSourceId(sourceId)
			.withSourceDisplayName(sourceDisplayName)
			.withSourceResourcePath(sourceResourcePath)
			.withSuggestedTargetId(suggestedTargetId)
			.withMatchReason(matchReason)
			.withCandidates(candidates);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getSourceId()).isEqualTo(sourceId);
		assertThat(bean.getSourceDisplayName()).isEqualTo(sourceDisplayName);
		assertThat(bean.getSourceResourcePath()).isEqualTo(sourceResourcePath);
		assertThat(bean.getSuggestedTargetId()).isEqualTo(suggestedTargetId);
		assertThat(bean.getMatchReason()).isEqualTo(matchReason);
		assertThat(bean.getCandidates()).isEqualTo(candidates);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LabelMapping.create()).hasAllNullFieldsOrProperties();
		assertThat(new LabelMapping()).hasAllNullFieldsOrProperties();
	}
}

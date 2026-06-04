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

class StatusMappingTest {

	@Test
	void testBean() {
		assertThat(StatusMapping.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var source = MetadataOption.create().withName("ONGOING").withDisplayName("Pågående");
		final var suggestedTarget = "IN_PROGRESS";
		final var matchReason = MatchReason.DISPLAY_NAME_EXACT;
		final var candidates = List.of(MetadataOption.create().withName("NEW_CASE").withDisplayName("Nytt ärende"));

		final var bean = StatusMapping.create()
			.withSource(source)
			.withSuggestedTarget(suggestedTarget)
			.withMatchReason(matchReason)
			.withCandidates(candidates);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getSource()).isEqualTo(source);
		assertThat(bean.getSuggestedTarget()).isEqualTo(suggestedTarget);
		assertThat(bean.getMatchReason()).isEqualTo(matchReason);
		assertThat(bean.getCandidates()).isEqualTo(candidates);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(StatusMapping.create()).hasAllNullFieldsOrProperties();
		assertThat(new StatusMapping()).hasAllNullFieldsOrProperties();
	}
}

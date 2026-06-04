package se.sundsvall.supportmanagement.api.model.handover;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ClassificationMappingTest {

	@Test
	void testBean() {
		assertThat(ClassificationMapping.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var source = ClassificationOption.create().withCategory("SUPPORT_CASE").withType("OTHER_ISSUES");
		final var suggestedCategory = "SUPPORT_CASE";
		final var suggestedType = "OTHER_ISSUES";
		final var candidates = Map.of("SUPPORT_CASE", List.of("OTHER_ISSUES", "QUESTION"));

		final var bean = ClassificationMapping.create()
			.withSource(source)
			.withSuggestedCategory(suggestedCategory)
			.withSuggestedType(suggestedType)
			.withCandidates(candidates);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getSource()).isEqualTo(source);
		assertThat(bean.getSuggestedCategory()).isEqualTo(suggestedCategory);
		assertThat(bean.getSuggestedType()).isEqualTo(suggestedType);
		assertThat(bean.getCandidates()).isEqualTo(candidates);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ClassificationMapping.create()).hasAllNullFieldsOrProperties();
		assertThat(new ClassificationMapping()).hasAllNullFieldsOrProperties();
	}
}

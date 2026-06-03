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

class MappingRequiredTest {

	@Test
	void testBean() {
		assertThat(MappingRequired.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var status = StatusMapping.create().withSuggestedTarget("IN_PROGRESS");
		final var classification = ClassificationMapping.create().withSuggestedCategory("SUPPORT_CASE");
		final var labels = List.of(LabelMapping.create().withSourceId("uuid-a"));
		final var contactReason = ContactReasonMapping.create().withSource("Bygglov");

		final var bean = MappingRequired.create()
			.withStatus(status)
			.withClassification(classification)
			.withLabels(labels)
			.withContactReason(contactReason);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getStatus()).isEqualTo(status);
		assertThat(bean.getClassification()).isEqualTo(classification);
		assertThat(bean.getLabels()).isEqualTo(labels);
		assertThat(bean.getContactReason()).isEqualTo(contactReason);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(MappingRequired.create()).hasAllNullFieldsOrProperties();
		assertThat(new MappingRequired()).hasAllNullFieldsOrProperties();
	}
}

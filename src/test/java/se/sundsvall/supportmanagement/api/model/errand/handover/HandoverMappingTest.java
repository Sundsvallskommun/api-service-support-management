package se.sundsvall.supportmanagement.api.model.errand.handover;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.Classification;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class HandoverMappingTest {

	@Test
	void testBean() {
		assertThat(HandoverMapping.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var status = "NEW_CASE";
		final var classification = Classification.create().withCategory("SUPPORT_CASE").withType("OTHER_ISSUES");
		final var labels = List.of("label-uuid-1");
		final var contactReason = "Printer issue";
		final var channel = "WEB_UI";
		final var activePhaseId = "phase-uuid";

		final var mapping = HandoverMapping.create()
			.withStatus(status)
			.withClassification(classification)
			.withLabels(labels)
			.withContactReason(contactReason)
			.withChannel(channel)
			.withActivePhaseId(activePhaseId);

		assertThat(mapping).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(mapping.getStatus()).isEqualTo(status);
		assertThat(mapping.getClassification()).isEqualTo(classification);
		assertThat(mapping.getLabels()).isEqualTo(labels);
		assertThat(mapping.getContactReason()).isEqualTo(contactReason);
		assertThat(mapping.getChannel()).isEqualTo(channel);
		assertThat(mapping.getActivePhaseId()).isEqualTo(activePhaseId);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(HandoverMapping.create()).hasAllNullFieldsOrProperties();
		assertThat(new HandoverMapping()).hasAllNullFieldsOrProperties();
	}
}

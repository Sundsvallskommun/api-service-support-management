package se.sundsvall.supportmanagement.api.model.errand.handover;

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

class HandoverErrandTest {

	@Test
	void testBean() {
		assertThat(HandoverErrand.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var newErrandId = "f0882f1d-06bc-47fd-b017-1d8307f5ce95";
		final var newErrandNumber = "KC-23010001";
		final var target = HandoverTarget.create().withNamespace("OTHER_NAMESPACE").withMunicipalityId("2281");
		final var relationId = "relation-uuid";
		final var appliedMappings = Map.of("status", "NEW_CASE");
		final var warnings = List.of("Some warning");

		final var response = HandoverErrand.create()
			.withNewErrandId(newErrandId)
			.withNewErrandNumber(newErrandNumber)
			.withTarget(target)
			.withRelationId(relationId)
			.withAppliedMappings(appliedMappings)
			.withWarnings(warnings);

		assertThat(response).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(response.getNewErrandId()).isEqualTo(newErrandId);
		assertThat(response.getNewErrandNumber()).isEqualTo(newErrandNumber);
		assertThat(response.getTarget()).isEqualTo(target);
		assertThat(response.getRelationId()).isEqualTo(relationId);
		assertThat(response.getAppliedMappings()).isEqualTo(appliedMappings);
		assertThat(response.getWarnings()).isEqualTo(warnings);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(HandoverErrand.create()).hasAllNullFieldsOrProperties();
		assertThat(new HandoverErrand()).hasAllNullFieldsOrProperties();
	}
}

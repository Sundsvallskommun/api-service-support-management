package se.sundsvall.supportmanagement.api.model.errand.handover;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class HandoverErrandRequestTest {

	@Test
	void testBean() {
		assertThat(HandoverErrandRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var target = HandoverTarget.create().withNamespace("OTHER_NAMESPACE").withMunicipalityId("2281");
		final var mapping = HandoverMapping.create().withStatus("NEW_CASE");
		final var overrides = HandoverOverrides.create().withTitle("title");
		final var include = HandoverInclude.create().withStakeholders(true);
		final var sourceHandling = HandoverSourceHandling.create().withAction(HandoverSourceAction.CLOSE);

		final var request = HandoverErrandRequest.create()
			.withTarget(target)
			.withMapping(mapping)
			.withOverrides(overrides)
			.withInclude(include)
			.withSourceHandling(sourceHandling);

		assertThat(request).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(request.getTarget()).isEqualTo(target);
		assertThat(request.getMapping()).isEqualTo(mapping);
		assertThat(request.getOverrides()).isEqualTo(overrides);
		assertThat(request.getInclude()).isEqualTo(include);
		assertThat(request.getSourceHandling()).isEqualTo(sourceHandling);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(HandoverErrandRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new HandoverErrandRequest()).hasAllNullFieldsOrProperties();
	}
}

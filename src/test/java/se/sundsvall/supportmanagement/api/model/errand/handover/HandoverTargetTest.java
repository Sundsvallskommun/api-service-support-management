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

class HandoverTargetTest {

	@Test
	void testBean() {
		assertThat(HandoverTarget.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var namespace = "OTHER_NAMESPACE";
		final var municipalityId = "2281";

		final var target = HandoverTarget.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId);

		assertThat(target).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(target.getNamespace()).isEqualTo(namespace);
		assertThat(target.getMunicipalityId()).isEqualTo(municipalityId);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(HandoverTarget.create()).hasAllNullFieldsOrProperties();
		assertThat(new HandoverTarget()).hasAllNullFieldsOrProperties();
	}
}

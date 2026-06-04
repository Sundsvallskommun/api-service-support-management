package se.sundsvall.supportmanagement.api.model.handover;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class HandoverPreviewRequestTest {

	@Test
	void testBean() {
		assertThat(HandoverPreviewRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var targetNamespace = "OTHER_NAMESPACE";
		final var targetMunicipalityId = "2281";

		final var bean = HandoverPreviewRequest.create()
			.withTargetNamespace(targetNamespace)
			.withTargetMunicipalityId(targetMunicipalityId);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getTargetNamespace()).isEqualTo(targetNamespace);
		assertThat(bean.getTargetMunicipalityId()).isEqualTo(targetMunicipalityId);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(HandoverPreviewRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new HandoverPreviewRequest()).hasAllNullFieldsOrProperties();
	}
}

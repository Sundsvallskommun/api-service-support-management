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

class LabelCandidateTest {

	@Test
	void testBean() {
		assertThat(LabelCandidate.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var id = "b82bd8ac-1507-4d9a-958d-369261eecc15";
		final var displayName = "Nyckelkort";
		final var resourcePath = "/access/keycard";

		final var bean = LabelCandidate.create()
			.withId(id)
			.withDisplayName(displayName)
			.withResourcePath(resourcePath);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
		assertThat(bean.getResourcePath()).isEqualTo(resourcePath);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LabelCandidate.create()).hasAllNullFieldsOrProperties();
		assertThat(new LabelCandidate()).hasAllNullFieldsOrProperties();
	}
}

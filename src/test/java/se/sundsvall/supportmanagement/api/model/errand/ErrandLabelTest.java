package se.sundsvall.supportmanagement.api.model.errand;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class ErrandLabelTest {

	@Test
	void testBean() {
		assertThat(ErrandLabel.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {

		final var classification = "classification";
		final var displayName = "displayName";
		final var id = "id";
		final var resourceName = "resourceName";
		final var resourcePath = "resourcePath";

		final var bean = ErrandLabel.create()
			.withClassification(classification)
			.withDisplayName(displayName)
			.withId(id)
			.withResourceName(resourceName)
			.withResourcePath(resourcePath);

		assertThat(bean.getClassification()).isEqualTo(classification);
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getResourceName()).isEqualTo(resourceName);
		assertThat(bean.getResourcePath()).isEqualTo(resourcePath);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandLabel.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandLabel()).hasAllNullFieldsOrProperties();
	}
}

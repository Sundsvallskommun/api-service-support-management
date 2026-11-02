package se.sundsvall.supportmanagement.api.model.errand;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.metadata.LabelAttribute;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

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

		final var attributes = List.of(LabelAttribute.create().withKey("key").withValue("value"));
		final var classification = "classification";
		final var classificationDisplayName = "classificationDisplayName";
		final var displayName = "displayName";
		final var id = "id";
		final var resourceName = "resourceName";
		final var resourcePath = "resourcePath";
		final var version = 1L;

		final var bean = ErrandLabel.create()
			.withAttributes(attributes)
			.withClassification(classification)
			.withClassificationDisplayName(classificationDisplayName)
			.withDisplayName(displayName)
			.withId(id)
			.withResourceName(resourceName)
			.withResourcePath(resourcePath)
			.withVersion(version);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getAttributes()).isEqualTo(attributes);
		assertThat(bean.getClassification()).isEqualTo(classification);
		assertThat(bean.getClassificationDisplayName()).isEqualTo(classificationDisplayName);
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getResourceName()).isEqualTo(resourceName);
		assertThat(bean.getResourcePath()).isEqualTo(resourcePath);
		assertThat(bean.getVersion()).isEqualTo(version);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandLabel.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandLabel()).hasAllNullFieldsOrProperties();
	}
}

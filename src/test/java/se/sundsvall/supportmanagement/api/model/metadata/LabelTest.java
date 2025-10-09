package se.sundsvall.supportmanagement.api.model.metadata;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LabelTest {

	@Test
	void testBean() {
		assertThat(Label.class, allOf(
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
		final var labels = List.of(Label.create());
		final var resourceName = "resourceName";
		final var resourcePath = "resourcePath";
		final var name = "name";

		final var bean = Label.create()
			.withClassification(classification)
			.withDisplayName(displayName)
			.withId(id)
			.withLabels(labels)
			.withName(name)
			.withResourceName(resourceName)
			.withResourcePath(resourcePath);

		assertThat(bean.getClassification()).isEqualTo(classification);
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getLabels()).isEqualTo(labels);
		assertThat(bean.getName()).isEqualTo(name);
		assertThat(bean.getResourceName()).isEqualTo(resourceName);
		assertThat(bean.getResourcePath()).isEqualTo(resourcePath);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Label.create()).hasAllNullFieldsOrProperties();
		assertThat(new Label()).hasAllNullFieldsOrProperties();
	}
}

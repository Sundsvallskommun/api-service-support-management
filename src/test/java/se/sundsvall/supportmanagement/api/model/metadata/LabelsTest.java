package se.sundsvall.supportmanagement.api.model.metadata;

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

class LabelsTest {

	@Test
	void testBean() {
		assertThat(Labels.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var labels = List.of(Label.create());

		final var bean = Labels.create()
			.withLabelStructure(labels);

		assertThat(bean.getLabelStructure()).isEqualTo(labels);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Labels.create()).hasAllNullFieldsOrProperties();
		assertThat(new Labels()).hasAllNullFieldsOrProperties();
	}
}

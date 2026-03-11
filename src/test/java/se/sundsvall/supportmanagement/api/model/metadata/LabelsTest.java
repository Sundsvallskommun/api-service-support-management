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

	@Test
	void testFlattenWithNestedLabels() {
		final var grandchild = Label.create().withId("grandchild-id").withClassification("subtype").withResourceName("GRANDCHILD").withDisplayName("Grandchild");
		final var child1 = Label.create().withId("child1-id").withClassification("type").withResourceName("CHILD_1").withDisplayName("Child 1").withLabels(List.of(grandchild));
		final var child2 = Label.create().withId("child2-id").withClassification("type").withResourceName("CHILD_2").withDisplayName("Child 2");
		final var root = Label.create().withId("root-id").withClassification("category").withResourceName("ROOT").withDisplayName("Root").withLabels(List.of(child1, child2));

		final var result = Labels.create().withLabelStructure(List.of(root)).flatten();

		assertThat(result).hasSize(4)
			.extracting(Label::getId)
			.containsExactly("root-id", "child1-id", "grandchild-id", "child2-id");
	}

	@Test
	void testFlattenWithFlatStructure() {
		final var label1 = Label.create().withId("id-1").withClassification("type").withResourceName("LABEL_1").withDisplayName("Label 1");
		final var label2 = Label.create().withId("id-2").withClassification("type").withResourceName("LABEL_2").withDisplayName("Label 2");

		final var result = Labels.create().withLabelStructure(List.of(label1, label2)).flatten();

		assertThat(result).hasSize(2)
			.extracting(Label::getId)
			.containsExactly("id-1", "id-2");
	}

	@Test
	void testFlattenWithNullStructure() {
		final var result = Labels.create().flatten();

		assertThat(result).isEmpty();
	}

	@Test
	void testFlattenWithEmptyStructure() {
		final var result = Labels.create().withLabelStructure(List.of()).flatten();

		assertThat(result).isEmpty();
	}
}

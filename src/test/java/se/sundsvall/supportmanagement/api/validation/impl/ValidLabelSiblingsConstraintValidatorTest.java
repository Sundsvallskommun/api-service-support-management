package se.sundsvall.supportmanagement.api.validation.impl;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.metadata.Label;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class ValidLabelSiblingsConstraintValidatorTest {

	private ValidLabelSiblingsConstraintValidator validator = new ValidLabelSiblingsConstraintValidator();

	@Test
	void oneLevelWithSameClassificationAndUniqueResourceName() {
		assertThat(validator.isValid(List.of(
			Label.create().withClassification("classification_1").withResourceName("resourceName_1"),
			Label.create().withClassification("classification_1").withResourceName("resourceName_2")), null)).isTrue();
	}

	@Test
	void oneLevelWithDifferentClassificationAndUniqueResourceName() {
		assertThat(validator.isValid(List.of(
			Label.create().withClassification("classification_1").withResourceName("resourceName_1"),
			Label.create().withClassification("classification_2").withResourceName("resourceName_2")), null)).isFalse();
	}

	@Test
	void oneLevelWithSameClassificationAndSameResourceName() {
		assertThat(validator.isValid(List.of(
			Label.create().withClassification("classification_1").withResourceName("resourceName_1"),
			Label.create().withClassification("classification_1").withResourceName("resourceName_1")), null)).isFalse();
	}

	@Test
	void subListWithSameClassificationAndUniqueResourceName() {
		final var root = Label.create()
			.withClassification("root_classification")
			.withLabels(List.of(
				Label.create().withClassification("classification_1").withResourceName("resourceName_1"),
				Label.create().withClassification("classification_1").withResourceName("resourceName_2")));

		assertThat(validator.isValid(List.of(root), null)).isTrue();
	}

	@Test
	void subListWithDifferentClassificationAndUniqueResourceName() {
		final var root = Label.create()
			.withClassification("root_classification")
			.withLabels(List.of(
				Label.create().withClassification("classification_1").withResourceName("resourceName_1"),
				Label.create().withClassification("classification_2").withResourceName("resourceName_2")));

		assertThat(validator.isValid(List.of(root), null)).isFalse();
	}

	@Test
	void subListWithSameClassificationAndSameResourceName() {
		final var root = Label.create()
			.withClassification("root_classification")
			.withLabels(List.of(
				Label.create().withClassification("classification_1").withResourceName("resourceName_1"),
				Label.create().withClassification("classification_1").withResourceName("resourceName_1")));

		assertThat(validator.isValid(List.of(root), null)).isFalse();
	}

	@Test
	void WithNullAsList() {
		assertThat(validator.isValid(null, null)).isTrue();
	}

	@Test
	void withEmptyList() {
		assertThat(validator.isValid(emptyList(), null)).isTrue();
	}
}

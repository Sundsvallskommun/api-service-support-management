package se.sundsvall.supportmanagement.api.validation.impl;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.metadata.Label;

class ValidLabelSiblingsConstraintValidatorTest {

	private ValidLabelSiblingsConstraintValidator validator = new ValidLabelSiblingsConstraintValidator();

	@Test
	void oneLevelWithSameClassificationAndUniqueName() {
		assertThat(validator.isValid(List.of(
			Label.create().withClassification("classification_1").withName("name_1"),
			Label.create().withClassification("classification_1").withName("name_2")), null)).isTrue();
	}

	@Test
	void oneLevelWithDifferentClassificationAndUniqueName() {
		assertThat(validator.isValid(List.of(
			Label.create().withClassification("classification_1").withName("name_1"),
			Label.create().withClassification("classification_2").withName("name_2")), null)).isFalse();
	}

	@Test
	void oneLevelWithSameClassificationAndSameName() {
		assertThat(validator.isValid(List.of(
			Label.create().withClassification("classification_1").withName("name_1"),
			Label.create().withClassification("classification_1").withName("name_1")), null)).isFalse();
	}

	@Test
	void subListWithSameClassificationAndUniqueName() {
		final var root = Label.create()
			.withClassification("root_classification")
			.withName("root_name")
			.withLabels(List.of(
				Label.create().withClassification("classification_1").withName("name_1"),
				Label.create().withClassification("classification_1").withName("name_2")));

		assertThat(validator.isValid(List.of(root), null)).isTrue();
	}

	@Test
	void subListWithDifferentClassificationAndUniqueName() {
		final var root = Label.create()
			.withClassification("root_classification")
			.withName("root_name")
			.withLabels(List.of(
				Label.create().withClassification("classification_1").withName("name_1"),
				Label.create().withClassification("classification_2").withName("name_2")));

		assertThat(validator.isValid(List.of(root), null)).isFalse();
	}

	@Test
	void subListWithSameClassificationAndSameName() {
		final var root = Label.create()
			.withClassification("root_classification")
			.withName("root_name")
			.withLabels(List.of(
				Label.create().withClassification("classification_1").withName("name_1"),
				Label.create().withClassification("classification_1").withName("name_1")));

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

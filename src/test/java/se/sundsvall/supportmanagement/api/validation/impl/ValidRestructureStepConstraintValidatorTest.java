package se.sundsvall.supportmanagement.api.validation.impl;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStep;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.ADD;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.DELETE;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.MERGE;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.MOVE;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.RENAME;

class ValidRestructureStepConstraintValidatorTest {

	private final ValidRestructureStepConstraintValidator validator = new ValidRestructureStepConstraintValidator();

	@Test
	void nullStepOrNullType_isValid() {
		assertThat(validator.isValid(null, null)).isTrue();
		assertThat(validator.isValid(LabelRestructureStep.create().withPath(List.of("A")), null)).isTrue();
	}

	@Test
	void add_withDisplayNameAndClassification_isValid() {
		assertThat(validator.isValid(LabelRestructureStep.create().withType(ADD).withPath(List.of("A")).withDisplayName("A").withClassification("CATEGORY"), null)).isTrue();
	}

	@Test
	void add_missingClassification_isInvalid() {
		assertThat(validator.isValid(LabelRestructureStep.create().withType(ADD).withPath(List.of("A")).withDisplayName("A"), null)).isFalse();
	}

	@Test
	void add_missingDisplayName_isInvalid() {
		assertThat(validator.isValid(LabelRestructureStep.create().withType(ADD).withPath(List.of("A")).withClassification("CATEGORY"), null)).isFalse();
	}

	@Test
	void add_withDestinationParentPath_isInvalid() {
		assertThat(validator.isValid(
			LabelRestructureStep.create().withType(ADD).withPath(List.of("A")).withDisplayName("A").withClassification("CATEGORY").withDestinationParentPath(List.of("B")), null))
			.isFalse();
	}

	@Test
	void rename_withDisplayName_isValid() {
		assertThat(validator.isValid(LabelRestructureStep.create().withType(RENAME).withPath(List.of("A")).withDisplayName("New name"), null)).isTrue();
	}

	@Test
	void rename_missingDisplayName_isInvalid() {
		assertThat(validator.isValid(LabelRestructureStep.create().withType(RENAME).withPath(List.of("A")), null)).isFalse();
	}

	@Test
	void rename_withClassification_isInvalid() {
		assertThat(validator.isValid(LabelRestructureStep.create().withType(RENAME).withPath(List.of("A")).withDisplayName("New name").withClassification("CATEGORY"), null)).isFalse();
	}

	@Test
	void delete_withOnlyPath_isValid() {
		assertThat(validator.isValid(LabelRestructureStep.create().withType(DELETE).withPath(List.of("A")), null)).isTrue();
	}

	@Test
	void delete_withDisplayName_isInvalid() {
		assertThat(validator.isValid(LabelRestructureStep.create().withType(DELETE).withPath(List.of("A")).withDisplayName("A"), null)).isFalse();
	}

	@Test
	void move_withOnlyPath_isValid() {
		assertThat(validator.isValid(LabelRestructureStep.create().withType(MOVE).withPath(List.of("A")), null)).isTrue();
	}

	@Test
	void move_withDestinationAndRename_isValid() {
		assertThat(validator.isValid(
			LabelRestructureStep.create().withType(MOVE).withPath(List.of("A")).withDestinationParentPath(List.of("B")).withNewResourceName("C").withDisplayName("New display"), null))
			.isTrue();
	}

	@Test
	void move_withClassification_isInvalid() {
		assertThat(validator.isValid(LabelRestructureStep.create().withType(MOVE).withPath(List.of("A")).withClassification("CATEGORY"), null)).isFalse();
	}

	@Test
	void move_withSourcePaths_isInvalid() {
		assertThat(validator.isValid(LabelRestructureStep.create().withType(MOVE).withPath(List.of("A")).withSourcePaths(List.of(List.of("B"))), null)).isFalse();
	}

	@Test
	void merge_withSourcePaths_isValid() {
		assertThat(validator.isValid(LabelRestructureStep.create().withType(MERGE).withPath(List.of("A")).withSourcePaths(List.of(List.of("B"), List.of("C"))), null)).isTrue();
	}

	@Test
	void merge_missingSourcePaths_isInvalid() {
		assertThat(validator.isValid(LabelRestructureStep.create().withType(MERGE).withPath(List.of("A")), null)).isFalse();
	}

	@Test
	void merge_withEmptySourcePath_isInvalid() {
		assertThat(validator.isValid(LabelRestructureStep.create().withType(MERGE).withPath(List.of("A")).withSourcePaths(List.of(List.of())), null)).isFalse();
	}

	@Test
	void merge_withDisplayName_isInvalid() {
		assertThat(validator.isValid(LabelRestructureStep.create().withType(MERGE).withPath(List.of("A")).withSourcePaths(List.of(List.of("B"))).withDisplayName("name"), null))
			.isFalse();
	}
}

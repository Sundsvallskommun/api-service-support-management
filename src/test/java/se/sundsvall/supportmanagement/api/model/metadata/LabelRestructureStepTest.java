package se.sundsvall.supportmanagement.api.model.metadata;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelRestructureStepTest {

	@Test
	void constructors() {
		assertThat(new LabelRestructureStep()).hasAllNullFieldsOrProperties();
		assertThat(LabelRestructureStep.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void gettersAndSetters() {
		var bean = new LabelRestructureStep();
		bean.setType(LabelRestructureStepType.MOVE);
		bean.setPath(List.of("CATEGORY", "TYPE"));
		bean.setDestinationParentPath(List.of("OTHER_CATEGORY"));
		bean.setSourcePaths(List.of(List.of("CATEGORY", "OLD_TYPE")));
		bean.setDisplayName("Display name");
		bean.setClassification("TYPE");
		bean.setNewResourceName("NEW_TYPE");

		assertThat(bean.getType()).isEqualTo(LabelRestructureStepType.MOVE);
		assertThat(bean.getPath()).containsExactly("CATEGORY", "TYPE");
		assertThat(bean.getDestinationParentPath()).containsExactly("OTHER_CATEGORY");
		assertThat(bean.getSourcePaths()).containsExactly(List.of("CATEGORY", "OLD_TYPE"));
		assertThat(bean.getDisplayName()).isEqualTo("Display name");
		assertThat(bean.getClassification()).isEqualTo("TYPE");
		assertThat(bean.getNewResourceName()).isEqualTo("NEW_TYPE");
	}

	@Test
	void withers() {
		var bean = LabelRestructureStep.create()
			.withType(LabelRestructureStepType.ADD)
			.withPath(List.of("CATEGORY"))
			.withDisplayName("Category")
			.withClassification("CATEGORY");

		assertThat(bean.getType()).isEqualTo(LabelRestructureStepType.ADD);
		assertThat(bean.getPath()).containsExactly("CATEGORY");
		assertThat(bean.getDisplayName()).isEqualTo("Category");
		assertThat(bean.getClassification()).isEqualTo("CATEGORY");
	}

	@Test
	void equalsAndHashCode() {
		var a = LabelRestructureStep.create().withType(LabelRestructureStepType.ADD).withPath(List.of("CATEGORY"));
		var b = LabelRestructureStep.create().withType(LabelRestructureStepType.ADD).withPath(List.of("CATEGORY"));
		var c = LabelRestructureStep.create().withType(LabelRestructureStepType.DELETE).withPath(List.of("CATEGORY"));

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(c);
	}

	@Test
	void toStringContainsFields() {
		var bean = LabelRestructureStep.create().withType(LabelRestructureStepType.ADD).withPath(List.of("CATEGORY"));
		assertThat(bean.toString()).contains("ADD", "CATEGORY");
	}
}

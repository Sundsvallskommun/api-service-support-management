package se.sundsvall.supportmanagement.api.model.metadata;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelRestructureStepResultTest {

	@Test
	void constructors() {
		assertThat(new LabelRestructureStepResult()).hasAllNullFieldsOrPropertiesExcept("index", "affectedErrandCount");
		assertThat(LabelRestructureStepResult.create()).hasAllNullFieldsOrPropertiesExcept("index", "affectedErrandCount");
	}

	@Test
	void gettersAndSetters() {
		var actions = List.of(AffectedAction.create().withId("id"));
		var bean = new LabelRestructureStepResult();
		bean.setIndex(1);
		bean.setType(LabelRestructureStepType.MOVE);
		bean.setPath(List.of("CATEGORY", "TYPE"));
		bean.setAffectedErrandCount(5L);
		bean.setAffectedActions(actions);

		assertThat(bean.getIndex()).isEqualTo(1);
		assertThat(bean.getType()).isEqualTo(LabelRestructureStepType.MOVE);
		assertThat(bean.getPath()).containsExactly("CATEGORY", "TYPE");
		assertThat(bean.getAffectedErrandCount()).isEqualTo(5L);
		assertThat(bean.getAffectedActions()).isEqualTo(actions);
	}

	@Test
	void withers() {
		var bean = LabelRestructureStepResult.create()
			.withIndex(2)
			.withType(LabelRestructureStepType.DELETE)
			.withPath(List.of("CATEGORY"))
			.withAffectedErrandCount(0L)
			.withAffectedActions(List.of());

		assertThat(bean.getIndex()).isEqualTo(2);
		assertThat(bean.getType()).isEqualTo(LabelRestructureStepType.DELETE);
		assertThat(bean.getPath()).containsExactly("CATEGORY");
		assertThat(bean.getAffectedErrandCount()).isZero();
		assertThat(bean.getAffectedActions()).isEmpty();
	}

	@Test
	void equalsAndHashCode() {
		var a = LabelRestructureStepResult.create().withIndex(0).withType(LabelRestructureStepType.ADD).withAffectedErrandCount(0L);
		var b = LabelRestructureStepResult.create().withIndex(0).withType(LabelRestructureStepType.ADD).withAffectedErrandCount(0L);
		var c = LabelRestructureStepResult.create().withIndex(1).withType(LabelRestructureStepType.ADD).withAffectedErrandCount(0L);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(c);
	}

	@Test
	void toStringContainsFields() {
		var bean = LabelRestructureStepResult.create().withIndex(3).withType(LabelRestructureStepType.MOVE);
		assertThat(bean.toString()).contains("3", "MOVE");
	}
}

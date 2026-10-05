package se.sundsvall.supportmanagement.api.model.metadata;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelRestructureDryRunResponseTest {

	@Test
	void constructors() {
		assertThat(new LabelRestructureDryRunResponse()).hasAllNullFieldsOrPropertiesExcept("totalAffectedErrandCount");
		assertThat(LabelRestructureDryRunResponse.create()).hasAllNullFieldsOrPropertiesExcept("totalAffectedErrandCount");
	}

	@Test
	void gettersAndSetters() {
		var steps = List.of(LabelRestructureStepResult.create().withIndex(0));
		var bean = new LabelRestructureDryRunResponse();
		bean.setTotalAffectedErrandCount(5L);
		bean.setSteps(steps);

		assertThat(bean.getTotalAffectedErrandCount()).isEqualTo(5L);
		assertThat(bean.getSteps()).isEqualTo(steps);
	}

	@Test
	void withers() {
		var steps = List.of(LabelRestructureStepResult.create().withIndex(0));
		var bean = LabelRestructureDryRunResponse.create()
			.withTotalAffectedErrandCount(3L)
			.withSteps(steps);

		assertThat(bean.getTotalAffectedErrandCount()).isEqualTo(3L);
		assertThat(bean.getSteps()).isEqualTo(steps);
	}

	@Test
	void equalsAndHashCode() {
		var a = LabelRestructureDryRunResponse.create().withTotalAffectedErrandCount(2L).withSteps(List.of());
		var b = LabelRestructureDryRunResponse.create().withTotalAffectedErrandCount(2L).withSteps(List.of());
		var c = LabelRestructureDryRunResponse.create().withTotalAffectedErrandCount(9L);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(c);
	}

	@Test
	void toStringContainsFields() {
		var bean = LabelRestructureDryRunResponse.create().withTotalAffectedErrandCount(7L);
		assertThat(bean.toString()).contains("7");
	}
}

package se.sundsvall.supportmanagement.api.model.metadata;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelRestructureRequestTest {

	@Test
	void constructors() {
		assertThat(new LabelRestructureRequest()).hasAllNullFieldsOrProperties();
		assertThat(LabelRestructureRequest.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void gettersAndSetters() {
		var steps = List.of(LabelRestructureStep.create().withType(LabelRestructureStepType.DELETE));
		var bean = new LabelRestructureRequest();
		bean.setSteps(steps);
		bean.setDryRun(true);

		assertThat(bean.getSteps()).isEqualTo(steps);
		assertThat(bean.getDryRun()).isTrue();
	}

	@Test
	void withers() {
		var steps = List.of(LabelRestructureStep.create().withType(LabelRestructureStepType.DELETE));
		var bean = LabelRestructureRequest.create()
			.withSteps(steps)
			.withDryRun(true);

		assertThat(bean.getSteps()).isEqualTo(steps);
		assertThat(bean.getDryRun()).isTrue();
	}

	@Test
	void equalsAndHashCode() {
		var a = LabelRestructureRequest.create().withDryRun(true).withSteps(List.of());
		var b = LabelRestructureRequest.create().withDryRun(true).withSteps(List.of());
		var c = LabelRestructureRequest.create().withDryRun(false);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(c);
	}

	@Test
	void toStringContainsFields() {
		var bean = LabelRestructureRequest.create().withDryRun(true);
		assertThat(bean.toString()).contains("true");
	}
}

package se.sundsvall.supportmanagement.api.model.metadata;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelMergeDryRunResponseTest {

	@Test
	void constructors() {
		assertThat(new LabelMergeDryRunResponse()).hasAllNullFieldsOrPropertiesExcept("affectedErrandCount");
		assertThat(LabelMergeDryRunResponse.create()).hasAllNullFieldsOrPropertiesExcept("affectedErrandCount");
	}

	@Test
	void gettersAndSetters() {
		var actions = List.of(AffectedAction.create().withId("id"));
		var bean = new LabelMergeDryRunResponse();
		bean.setAffectedErrandCount(5L);
		bean.setAffectedActions(actions);

		assertThat(bean.getAffectedErrandCount()).isEqualTo(5L);
		assertThat(bean.getAffectedActions()).isEqualTo(actions);
	}

	@Test
	void withers() {
		var actions = List.of(AffectedAction.create().withId("id"));
		var bean = LabelMergeDryRunResponse.create()
			.withAffectedErrandCount(3L)
			.withAffectedActions(actions);

		assertThat(bean.getAffectedErrandCount()).isEqualTo(3L);
		assertThat(bean.getAffectedActions()).isEqualTo(actions);
	}

	@Test
	void equalsAndHashCode() {
		var a = LabelMergeDryRunResponse.create().withAffectedErrandCount(2L).withAffectedActions(List.of());
		var b = LabelMergeDryRunResponse.create().withAffectedErrandCount(2L).withAffectedActions(List.of());
		var c = LabelMergeDryRunResponse.create().withAffectedErrandCount(9L);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(c);
	}

	@Test
	void toStringContainsFields() {
		var bean = LabelMergeDryRunResponse.create().withAffectedErrandCount(7L);
		assertThat(bean.toString()).contains("7");
	}
}

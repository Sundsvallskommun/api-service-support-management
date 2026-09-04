package se.sundsvall.supportmanagement.api.model.metadata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelMoveRequestTest {

	@Test
	void constructors() {
		assertThat(new LabelMoveRequest()).hasAllNullFieldsOrPropertiesExcept("dryRun");
		assertThat(LabelMoveRequest.create()).hasAllNullFieldsOrPropertiesExcept("dryRun");
	}

	@Test
	void gettersAndSetters() {
		var bean = new LabelMoveRequest();
		bean.setNewParentId("parent-id");
		bean.setDryRun(true);

		assertThat(bean.getNewParentId()).isEqualTo("parent-id");
		assertThat(bean.isDryRun()).isTrue();
	}

	@Test
	void withers() {
		var bean = LabelMoveRequest.create()
			.withNewParentId("parent-id")
			.withDryRun(true);

		assertThat(bean.getNewParentId()).isEqualTo("parent-id");
		assertThat(bean.isDryRun()).isTrue();
	}

	@Test
	void equalsAndHashCode() {
		var a = LabelMoveRequest.create().withNewParentId("id").withDryRun(true);
		var b = LabelMoveRequest.create().withNewParentId("id").withDryRun(true);
		var c = LabelMoveRequest.create().withNewParentId("other").withDryRun(false);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(c);
	}

	@Test
	void toStringContainsFields() {
		var bean = LabelMoveRequest.create().withNewParentId("parent-id").withDryRun(true);
		assertThat(bean.toString()).contains("parent-id", "true");
	}
}

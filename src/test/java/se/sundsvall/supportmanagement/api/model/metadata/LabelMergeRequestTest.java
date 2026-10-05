package se.sundsvall.supportmanagement.api.model.metadata;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelMergeRequestTest {

	@Test
	void constructors() {
		assertThat(new LabelMergeRequest()).hasAllNullFieldsOrProperties();
		assertThat(LabelMergeRequest.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void gettersAndSetters() {
		var bean = new LabelMergeRequest();
		bean.setSourceLabelIds(List.of("source-id"));
		bean.setDryRun(true);

		assertThat(bean.getSourceLabelIds()).containsExactly("source-id");
		assertThat(bean.getDryRun()).isTrue();
	}

	@Test
	void withers() {
		var bean = LabelMergeRequest.create()
			.withSourceLabelIds(List.of("source-id"))
			.withDryRun(true);

		assertThat(bean.getSourceLabelIds()).containsExactly("source-id");
		assertThat(bean.getDryRun()).isTrue();
	}

	@Test
	void equalsAndHashCode() {
		var a = LabelMergeRequest.create().withSourceLabelIds(List.of("id")).withDryRun(true);
		var b = LabelMergeRequest.create().withSourceLabelIds(List.of("id")).withDryRun(true);
		var c = LabelMergeRequest.create().withSourceLabelIds(List.of("other")).withDryRun(false);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(c);
	}

	@Test
	void toStringContainsFields() {
		var bean = LabelMergeRequest.create().withSourceLabelIds(List.of("source-id")).withDryRun(true);
		assertThat(bean.toString()).contains("source-id", "true");
	}
}

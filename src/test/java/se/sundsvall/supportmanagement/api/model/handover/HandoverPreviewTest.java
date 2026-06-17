package se.sundsvall.supportmanagement.api.model.handover;

import java.util.List;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class HandoverPreviewTest {

	@Test
	void testBean() {
		assertThat(HandoverPreview.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var directlyCopyable = DirectlyCopyable.create().withTitle("title");
		final var mappingRequired = MappingRequired.create().withStatus(StatusMapping.create().withSuggestedTarget("IN_PROGRESS"));
		final var sourceHandling = SourceHandling.create().withStatusCandidates(List.of(MetadataOption.create().withName("SOLVED").withDisplayName("Löst")));
		final var notCopyable = List.of(NotCopyable.create().withField("phases").withReason("Phase history is source-specific"));
		final var warnings = List.of(Warning.create().withType(WarningType.ROLE_NOT_IN_TARGET).withValue("EXTERNAL_REPORTER"));

		final var bean = HandoverPreview.create()
			.withDirectlyCopyable(directlyCopyable)
			.withMappingRequired(mappingRequired)
			.withSourceHandling(sourceHandling)
			.withNotCopyable(notCopyable)
			.withWarnings(warnings);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getDirectlyCopyable()).isEqualTo(directlyCopyable);
		assertThat(bean.getMappingRequired()).isEqualTo(mappingRequired);
		assertThat(bean.getSourceHandling()).isEqualTo(sourceHandling);
		assertThat(bean.getNotCopyable()).isEqualTo(notCopyable);
		assertThat(bean.getWarnings()).isEqualTo(warnings);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(HandoverPreview.create()).hasAllNullFieldsOrProperties();
		assertThat(new HandoverPreview()).hasAllNullFieldsOrProperties();
	}
}

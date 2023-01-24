package se.sundsvall.supportmanagement.api.model.attachment;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ErrandAttachmentTest {

	@Test
	void testBean() {
		assertThat(ErrandAttachment.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "id";
		final var fileName = "fileName";
		final var mimeType = "mimeType";
		final var base64EncodedString = "file";
		final var errandAttachmentHeader = ErrandAttachmentHeader.create().withFileName(fileName).withId(id);

		final var bean = ErrandAttachment.create()
			.withErrandAttachmentHeader(errandAttachmentHeader)
			.withMimeType(mimeType)
			.withBase64EncodedString(base64EncodedString);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getErrandAttachmentHeader()).isEqualTo(errandAttachmentHeader);
		assertThat(bean.getMimeType()).isEqualTo(mimeType);
		assertThat(bean.getBase64EncodedString()).isEqualTo(base64EncodedString);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandAttachment.create()).hasAllNullFieldsOrProperties();
	}
}

package se.sundsvall.supportmanagement.api.model.attachment;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

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

		final var bean = ErrandAttachment.create()
			.withId(id)
			.withFileName(fileName)
			.withMimeType(mimeType);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getFileName()).isEqualTo(fileName);
		assertThat(bean.getMimeType()).isEqualTo(mimeType);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandAttachment.create()).hasAllNullFieldsOrProperties();
	}
}

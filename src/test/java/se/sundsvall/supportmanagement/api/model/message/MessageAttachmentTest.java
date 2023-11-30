package se.sundsvall.supportmanagement.api.model.message;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class MessageAttachmentTest {

	@Test
	void testBean() {
		assertThat(MessageAttachment.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var attachmentID = "attachmentID";
		final var name = "name";
		final var contentType = "contentType";

		final var bean = MessageAttachment.create()
			.withAttachmentID(attachmentID)
			.withName(name)
			.withContentType(contentType);

		assertThat(bean.getAttachmentID()).isEqualTo(attachmentID);
		assertThat(bean.getName()).isEqualTo(name);
		assertThat(bean.getContentType()).isEqualTo(contentType);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(MessageAttachment.create()).hasAllNullFieldsOrProperties();
		assertThat(new MessageAttachment()).hasAllNullFieldsOrProperties();
	}

}

package se.sundsvall.supportmanagement.api.model.communication;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

class WebMessageRequestTest {

	@Test
	void testBean() {
		assertThat(WebMessageRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var message = "message";
		final var attachments = List.of(WebMessageAttachment.create());
		final var attachmentsIds = List.of("1", "2");

		final var result = WebMessageRequest.create()
			.withMessage(message)
			.withAttachments(attachments)
			.withAttachmentIds(attachmentsIds);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getMessage()).isEqualTo(message);
		assertThat(result.getAttachments()).isSameAs(attachments);
		assertThat(result.getAttachmentIds()).isSameAs(attachmentsIds);

	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(WebMessageRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new WebMessageRequest()).hasAllNullFieldsOrProperties();
	}
}
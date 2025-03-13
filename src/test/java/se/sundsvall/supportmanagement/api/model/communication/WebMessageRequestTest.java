package se.sundsvall.supportmanagement.api.model.communication;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

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
		final var internal = true;

		final var result = WebMessageRequest.create()
			.withMessage(message)
			.withAttachments(attachments)
			.withAttachmentIds(attachmentsIds)
			.withInternal(internal);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getMessage()).isEqualTo(message);
		assertThat(result.getAttachments()).isSameAs(attachments);
		assertThat(result.getAttachmentIds()).isSameAs(attachmentsIds);
		assertThat(result.getInternal()).isEqualTo(internal);

	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(WebMessageRequest.create()).hasAllNullFieldsOrPropertiesExcept("internal", "dispatch");
		assertThat(new WebMessageRequest()).hasAllNullFieldsOrPropertiesExcept("internal", "dispatch");
	}
}

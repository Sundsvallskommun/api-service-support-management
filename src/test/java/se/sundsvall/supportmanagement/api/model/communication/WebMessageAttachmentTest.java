package se.sundsvall.supportmanagement.api.model.communication;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class WebMessageAttachmentTest {

	@Test
	void testBean() {
		assertThat(WebMessageAttachment.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var base64EncodedString = "ZGF0YQ==";
		final var name = "name";

		final var bean = WebMessageAttachment.create()
			.withBase64EncodedString(base64EncodedString)
			.withFileName(name);

		Assertions.assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		Assertions.assertThat(bean.getBase64EncodedString()).isEqualTo(base64EncodedString);
		Assertions.assertThat(bean.getFileName()).isEqualTo(name);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		Assertions.assertThat(WebMessageAttachment.create()).hasAllNullFieldsOrProperties();
		Assertions.assertThat(new WebMessageAttachment()).hasAllNullFieldsOrProperties();
	}
}

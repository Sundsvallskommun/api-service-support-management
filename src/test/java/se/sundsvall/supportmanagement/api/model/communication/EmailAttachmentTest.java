package se.sundsvall.supportmanagement.api.model.communication;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class EmailAttachmentTest {

	@Test
	void testBean() {
		assertThat(EmailAttachment.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var string = "string";
		final var base64EncodedString = encodeBase64String(string.getBytes());
		final var name = "name";

		final var bean = EmailAttachment.create()
			.withBase64EncodedString(base64EncodedString)
			.withFileName(name);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getBase64EncodedString()).isEqualTo(base64EncodedString);
		assertThat(new String(decodeBase64(bean.getBase64EncodedString()))).isEqualTo(string);
		assertThat(bean.getFileName()).isEqualTo(name);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(EmailAttachment.create()).hasAllNullFieldsOrProperties();
		assertThat(new EmailAttachment()).hasAllNullFieldsOrProperties();
	}
}

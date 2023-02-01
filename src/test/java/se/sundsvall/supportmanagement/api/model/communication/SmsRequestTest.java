package se.sundsvall.supportmanagement.api.model.communication;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class SmsRequestTest {

	@Test
	void testBean() {
		assertThat(SmsRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var message = "message";
		final var recipient = "recipient";
		final var sender = "sender";

		final var bean = SmsRequest.create()
			.withMessage(message)
			.withRecipient(recipient)
			.withSender(sender);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getMessage()).isEqualTo(message);
		assertThat(bean.getRecipient()).isEqualTo(recipient);
		assertThat(bean.getSender()).isEqualTo(sender);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(SmsRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new SmsRequest()).hasAllNullFieldsOrProperties();
	}
}

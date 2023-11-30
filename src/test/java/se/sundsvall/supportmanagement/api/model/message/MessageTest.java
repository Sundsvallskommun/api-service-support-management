package se.sundsvall.supportmanagement.api.model.message;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MessageTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(Message.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "id";
		final var direction = Direction.INBOUND;
		final var messageBody = "messageBody";
		final var sent = OffsetDateTime.now();
		final var errandNumber = "errandNumber";
		final var subject = "subject";
		final var username = "username";
		final var firstName = "firstName";
		final var lastName = "lastName";
		final var messageType = MessageType.EMAIL;
		final var mobileNumber = "mobileNumber";
		final var email = "email";
		final var userID = "userID";
		final var viewed = true;

		final var messageAttachments = List.of(MessageAttachment.create());

		final var bean = Message.create()
			.withMessageID(id)
			.withDirection(direction)
			.withMessageBody(messageBody)
			.withSent(sent)
			.withErrandNumber(errandNumber)
			.withSubject(subject)
			.withUsername(username)
			.withFirstName(firstName)
			.withLastName(lastName)
			.withMessageType(messageType)
			.withMobileNumber(mobileNumber)
			.withEmail(email)
			.withUserID(userID)
			.withViewed(viewed)
			.withMessageAttachments(messageAttachments);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getMessageID()).isEqualTo(id);
		assertThat(bean.getDirection()).isEqualTo(direction);
		assertThat(bean.getMessageBody()).isEqualTo(messageBody);
		assertThat(bean.getSent()).isEqualTo(sent);
		assertThat(bean.getErrandNumber()).isEqualTo(errandNumber);
		assertThat(bean.getSubject()).isEqualTo(subject);
		assertThat(bean.getUsername()).isEqualTo(username);
		assertThat(bean.getFirstName()).isEqualTo(firstName);
		assertThat(bean.getLastName()).isEqualTo(lastName);
		assertThat(bean.getMessageType()).isEqualTo(messageType);
		assertThat(bean.getMobileNumber()).isEqualTo(mobileNumber);
		assertThat(bean.getEmail()).isEqualTo(email);
		assertThat(bean.getUserID()).isEqualTo(userID);
		assertThat(bean.isViewed()).isEqualTo(viewed);
		assertThat(bean.getMessageAttachments()).isEqualTo(messageAttachments);

	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Message.create()).hasAllNullFieldsOrPropertiesExcept("viewed");
		assertThat(new Message()).hasAllNullFieldsOrPropertiesExcept("viewed");
	}

}

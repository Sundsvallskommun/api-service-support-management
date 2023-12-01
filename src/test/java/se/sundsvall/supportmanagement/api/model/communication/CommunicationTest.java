package se.sundsvall.supportmanagement.api.model.communication;

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

import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;

class CommunicationTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(Communication.class, allOf(
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
		final var messageType = CommunicationType.EMAIL;
		final var mobileNumber = "mobileNumber";
		final var email = "email";
		final var viewed = true;

		final var communicationAttachments = List.of(CommunicationAttachment.create());

		final var bean = Communication.create()
			.withCommunicationID(id)
			.withDirection(direction)
			.withMessageBody(messageBody)
			.withSent(sent)
			.withErrandNumber(errandNumber)
			.withSubject(subject)
			.withCommunicationType(messageType)
			.withMobileNumber(mobileNumber)
			.withEmail(email)
			.withViewed(viewed)
			.withCommunicationAttachments(communicationAttachments);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getCommunicationID()).isEqualTo(id);
		assertThat(bean.getDirection()).isEqualTo(direction);
		assertThat(bean.getMessageBody()).isEqualTo(messageBody);
		assertThat(bean.getSent()).isEqualTo(sent);
		assertThat(bean.getErrandNumber()).isEqualTo(errandNumber);
		assertThat(bean.getSubject()).isEqualTo(subject);
		assertThat(bean.getCommunicationType()).isEqualTo(messageType);
		assertThat(bean.getMobileNumber()).isEqualTo(mobileNumber);
		assertThat(bean.getEmail()).isEqualTo(email);
		assertThat(bean.isViewed()).isEqualTo(viewed);
		assertThat(bean.getCommunicationAttachments()).isEqualTo(communicationAttachments);

	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Communication.create()).hasAllNullFieldsOrPropertiesExcept("viewed");
		assertThat(new Communication()).hasAllNullFieldsOrPropertiesExcept("viewed");
	}

}

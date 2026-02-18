package se.sundsvall.supportmanagement.api.model.communication;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;

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
		// Arrange
		final var id = "id";
		final var sender = "sender";
		final var direction = Direction.INBOUND;
		final var messageBody = "messageBody";
		final var htmlMessageBody = "htmlMessageBody";
		final var sent = OffsetDateTime.now();
		final var errandNumber = "errandNumber";
		final var subject = "subject";
		final var messageType = CommunicationType.EMAIL;
		final var target = "target";
		final var viewed = true;
		final var emailHeaders = Map.of(EmailHeader.REFERENCES, List.of("reference1", "reference2"));
		final var communicationAttachments = List.of(CommunicationAttachment.create());
		final var internal = true;

		// Act
		final var bean = Communication.create()
			.withCommunicationID(id)
			.withSender(sender)
			.withDirection(direction)
			.withMessageBody(messageBody)
			.withHtmlMessageBody(htmlMessageBody)
			.withSent(sent)
			.withErrandNumber(errandNumber)
			.withSubject(subject)
			.withCommunicationType(messageType)
			.withTarget(target)
			.withRecipients(List.of(target))
			.withViewed(viewed)
			.withCommunicationAttachments(communicationAttachments)
			.withEmailHeaders(emailHeaders)
			.withInternal(internal);
		// Assert
		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getCommunicationID()).isEqualTo(id);
		assertThat(bean.getSender()).isEqualTo(sender);
		assertThat(bean.getDirection()).isEqualTo(direction);
		assertThat(bean.getMessageBody()).isEqualTo(messageBody);
		assertThat(bean.getHtmlMessageBody()).isEqualTo(htmlMessageBody);
		assertThat(bean.getSent()).isEqualTo(sent);
		assertThat(bean.getErrandNumber()).isEqualTo(errandNumber);
		assertThat(bean.getSubject()).isEqualTo(subject);
		assertThat(bean.getCommunicationType()).isEqualTo(messageType);
		assertThat(bean.getTarget()).isEqualTo(target);
		assertThat(bean.getRecipients()).isEqualTo(List.of(target));
		assertThat(bean.getViewed()).isEqualTo(viewed);
		assertThat(bean.getCommunicationAttachments()).isEqualTo(communicationAttachments);
		assertThat(bean.getEmailHeaders()).isEqualTo(emailHeaders);
		assertThat(bean.isInternal()).isEqualTo(internal);

	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Communication.create()).hasAllNullFieldsOrPropertiesExcept("viewed", "internal");
		assertThat(new Communication()).hasAllNullFieldsOrPropertiesExcept("viewed", "internal");
	}

}

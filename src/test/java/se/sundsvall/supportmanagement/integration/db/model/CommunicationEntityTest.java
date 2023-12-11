package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;

class CommunicationEntityTest {

	@BeforeAll
	static void setup() {
		final var random = new Random();
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(random.nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(CommunicationEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var communicationId = "communicationId";
		final var errandNumber = "errandNumber";
		final var direction = Direction.OUTBOUND;
		final var externalCaseID = "externalCaseID";
		final var subject = "subject";
		final var messageBody = "messageBody";
		final var sent = OffsetDateTime.now();
		final var communicationType = CommunicationType.EMAIL;
		final var mobileNumber = "mobileNumber";
		final var email = "email";
		final var viewed = true;
		final var attachments = List.of(CommunicationAttachmentEntity.create());

		final var entity = CommunicationEntity.create()
			.withCommunicationID(communicationId)
			.withErrandNumber(errandNumber)
			.withDirection(direction)
			.withExternalCaseID(externalCaseID)
			.withSubject(subject)
			.withMessageBody(messageBody)
			.withSent(sent)
			.withCommunicationType(communicationType)
			.withMobileNumber(mobileNumber)
			.withEmail(email)
			.withViewed(viewed)
			.withAttachments(attachments);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getCommunicationID()).isEqualTo(communicationId);
		assertThat(entity.getErrandNumber()).isEqualTo(errandNumber);
		assertThat(entity.getDirection()).isEqualTo(direction);
		assertThat(entity.getExternalCaseID()).isEqualTo(externalCaseID);
		assertThat(entity.getSubject()).isEqualTo(subject);
		assertThat(entity.getMessageBody()).isEqualTo(messageBody);
		assertThat(entity.getSent()).isEqualTo(sent);
		assertThat(entity.getCommunicationType()).isEqualTo(communicationType);
		assertThat(entity.getMobileNumber()).isEqualTo(mobileNumber);
		assertThat(entity.getEmail()).isEqualTo(email);
		assertThat(entity.isViewed()).isEqualTo(viewed);
		assertThat(entity.getAttachments()).isEqualTo(attachments);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(CommunicationEntity.create()).hasAllNullFieldsOrPropertiesExcept("viewed");
		assertThat(new CommunicationEntity()).hasAllNullFieldsOrPropertiesExcept("viewed");
	}

}

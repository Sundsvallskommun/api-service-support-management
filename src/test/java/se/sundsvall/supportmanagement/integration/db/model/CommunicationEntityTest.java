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
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;

class CommunicationEntityTest {

	@BeforeAll
	static void setup() {
		final var random = new Random();
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(random.nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> List.of(CommunicationAttachmentEntity.create().withId(UUID.randomUUID().toString())), List.class);
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

		final var id = "id";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var sender = "sender";
		final var errandNumber = "errandNumber";
		final var direction = Direction.OUTBOUND;
		final var externalCaseID = "externalCaseID";
		final var subject = "subject";
		final var messageBody = "messageBody";
		final var sent = OffsetDateTime.now();
		final var type = CommunicationType.EMAIL;
		final var target = "target";
		final var viewed = true;
		final var attachments = List.of(CommunicationAttachmentEntity.create());
		final var errandAttachments = List.of(AttachmentEntity.create());
		final var emailHeaders = List.of(CommunicationEmailHeaderEntity.create()
			.withHeader(EmailHeader.IN_REPLY_TO)
			.withValues(List.of("someValue")));

		final var entity = CommunicationEntity.create()
			.withId(id)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withSender(sender)
			.withErrandNumber(errandNumber)
			.withDirection(direction)
			.withExternalId(externalCaseID)
			.withSubject(subject)
			.withMessageBody(messageBody)
			.withSent(sent)
			.withType(type)
			.withTarget(target)
			.withViewed(viewed)
			.withErrandAttachments(errandAttachments)
			.withAttachments(attachments)
			.withEmailHeaders(emailHeaders);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getSender()).isEqualTo(sender);
		assertThat(entity.getErrandNumber()).isEqualTo(errandNumber);
		assertThat(entity.getDirection()).isEqualTo(direction);
		assertThat(entity.getExternalId()).isEqualTo(externalCaseID);
		assertThat(entity.getSubject()).isEqualTo(subject);
		assertThat(entity.getMessageBody()).isEqualTo(messageBody);
		assertThat(entity.getSent()).isEqualTo(sent);
		assertThat(entity.getType()).isEqualTo(type);
		assertThat(entity.getTarget()).isEqualTo(target);
		assertThat(entity.getErrandAttachments()).isEqualTo(errandAttachments);
		assertThat(entity.isViewed()).isEqualTo(viewed);
		assertThat(entity.getAttachments()).isEqualTo(attachments);
		assertThat(entity.getAttachments()).allMatch(attachment -> attachment.getCommunicationEntity() == entity);
		assertThat(entity.getEmailHeaders()).isEqualTo(emailHeaders);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(CommunicationEntity.create()).hasAllNullFieldsOrPropertiesExcept("viewed");
		assertThat(new CommunicationEntity()).hasAllNullFieldsOrPropertiesExcept("viewed");
	}

}

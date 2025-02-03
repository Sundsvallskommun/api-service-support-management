package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.Direction.INBOUND;

import generated.se.sundsvall.emailreader.Email;
import generated.se.sundsvall.emailreader.EmailAttachment;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import javax.sql.rowset.serial.SerialBlob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;
import se.sundsvall.supportmanagement.service.util.BlobBuilder;

@ExtendWith(MockitoExtension.class)
class EmailReaderMapperTest {

	@Mock
	private BlobBuilder blobBuilderMock;

	@InjectMocks
	private EmailReaderMapper emailReaderMapper;

	@Test
	void toCommunicationEntity() {

		// Arrange
		final var errandEntity = ErrandEntity.create()
			.withNamespace("someNamespace")
			.withMunicipalityId("someMunicipalityId")
			.withErrandNumber("someErrandNumber");

		final var email = new Email()
			.id("someId")
			.subject("someSubject")
			.recipients(List.of("someRecipient"))
			.sender("someSender")
			.message("someMessage")
			.headers(Map.of(EmailHeader.MESSAGE_ID.toString(), List.of("someValue")))
			.receivedAt(OffsetDateTime.now())
			.attachments(List.of(new EmailAttachment()
				.name("someName")
				.contentType("text/plain")
				.id(99L)));

		// Act
		final var result = emailReaderMapper.toCommunicationEntity(email, errandEntity);

		// Assert
		assertThat(result).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "errandNumber", "errandAttachments", "senderUserId", "attachmentData");
		assertThat(result.getSubject()).isEqualTo("someSubject");
		assertThat(result.getExternalId()).isEmpty();
		assertThat(result.getErrandNumber()).isEqualTo("someErrandNumber");
		assertThat(result.getSent()).isNotNull().isCloseTo(OffsetDateTime.now(), org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.SECONDS));
		assertThat(result.getDirection()).isEqualTo(INBOUND);
		assertThat(result.getType()).isEqualTo(CommunicationType.EMAIL);
		assertThat(result.getTarget()).isEqualTo("someRecipient");
		assertThat(result.getMessageBody()).isEqualTo("someMessage");
		assertThat(result.getSender()).isEqualTo("someSender");
		assertThat(result.getAttachments()).isNotNull().hasSize(1);
		assertThat(result.getAttachments().getFirst().getName()).isEqualTo("someName");
		assertThat(result.getAttachments().getFirst().getContentType()).isEqualTo("text/plain");
		assertThat(result.getAttachments().getFirst().getAttachmentData()).isNull();
		assertThat(result.getAttachments().getFirst().getForeignId()).isEqualTo("99");
		assertThat(result.getEmailHeaders()).isNotNull().hasSize(1);
		assertThat(result.getEmailHeaders().getFirst().getHeader()).isEqualTo(EmailHeader.MESSAGE_ID);
		assertThat(result.getEmailHeaders().getFirst().getValues()).isNotNull().hasSize(1).contains("someValue");
	}

	@Test
	void toCommunicationEntityWhenRecipientsIsNull() {

		// Arrange
		final var errandEntity = ErrandEntity.create()
			.withNamespace("someNamespace")
			.withMunicipalityId("someMunicipalityId")
			.withErrandNumber("someErrandNumber");

		final var email = new Email()
			.id("someId")
			.subject("someSubject")
			.recipients(null)
			.sender("someSender")
			.message("someMessage")
			.headers(Map.of(EmailHeader.MESSAGE_ID.toString(), List.of("someValue")))
			.receivedAt(OffsetDateTime.now())
			.attachments(List.of(new EmailAttachment()
				.name("someName")
				.contentType("text/plain")
				.id(99L)));

		// Act
		final var result = emailReaderMapper.toCommunicationEntity(email, errandEntity);

		// Assert
		assertThat(result).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "target", "errandNumber", "errandAttachments", "senderUserId");
		assertThat(result.getTarget()).isNull();
		assertThat(result.getErrandNumber()).isEqualTo("someErrandNumber");
		assertThat(result.getAttachments()).isNotNull().hasSize(1);
		assertThat(result.getAttachments().getFirst()).hasNoNullFieldsOrPropertiesExcept("id", "fileSize", "attachmentData");
	}

	@Test
	void toCommunicationEntity_null() {

		final var result = emailReaderMapper.toCommunicationEntity(null, null);

		assertThat(result).isNull();
	}

	@ParameterizedTest
	@CsvSource({
		"true,role", "false,role", "true,null"
	})
	void toErrand(final boolean addSenderAsStakeholder, final String stakeholderRole) {

		final var email = new Email()
			.id("someId")
			.subject("someSubject")
			.recipients(List.of("someRecipient"))
			.sender("someSender")
			.message("someMessage")
			.receivedAt(OffsetDateTime.now())
			.metadata(Map.of("classification.category", "someCategory", "classification.type", "someType", "labels", "someLabel1;someLabel2"))
			.attachments(null);
		final var status = "NEW";

		final var result = emailReaderMapper.toErrand(email, status, addSenderAsStakeholder, stakeholderRole, "errandChannel");

		assertThat(result).isNotNull();
		assertThat(result.getTitle()).isEqualTo("someSubject");
		assertThat(result.getDescription()).isEqualTo("someMessage");
		assertThat(result.getStatus()).isEqualTo(status);
		assertThat(result.getClassification()).isNotNull();
		assertThat(result.getClassification().getCategory()).isEqualTo("someCategory");
		assertThat(result.getClassification().getType()).isEqualTo("someType");
		assertThat(result.getChannel()).isEqualTo("errandChannel");
		assertThat(result.getLabels()).containsExactly("someLabel1", "someLabel2");

		if (addSenderAsStakeholder) {
			assertThat(result.getStakeholders()).isNotNull().hasSize(1);
			assertThat(result.getStakeholders().getFirst().getRole()).isEqualTo(stakeholderRole);
			assertThat(result.getStakeholders().getFirst().getContactChannels()).isNotNull().hasSize(1);
			assertThat(result.getStakeholders().getFirst().getContactChannels().getFirst().getType()).isEqualTo("EMAIL");
		} else {
			assertThat(result.getStakeholders()).isNull();
		}
	}

	@Test
	void createEmailRequest() {
		// Arrange
		final var subject = "someSubject";
		final var recipients = List.of("someRecipient");
		final var message = "someMessage";
		final var receivedAt = OffsetDateTime.now();
		final var sender = "someSender";
		final var template = "someTemplate";
		final Map<String, List<String>> emailHeaders = Map.of("MESSAGE_ID", List.of("someValue"), "IN_REPLY_TO", List.of("someValue"));

		final var email = new Email()
			.subject(subject)
			.recipients(recipients)
			.sender(sender)
			.message(message)
			.headers(emailHeaders)
			.receivedAt(receivedAt);
		// Act
		final var result = emailReaderMapper.createEmailRequest(email, sender, template, subject);

		// Assert
		assertThat(result).isNotNull().hasNoNullFieldsOrPropertiesExcept("senderName", "htmlMessage", "attachments", "attachmentIds");
		assertThat(result.getMessage()).isEqualTo(template);
		assertThat(result.getSubject()).isEqualTo(subject);
		assertThat(result.getRecipient()).isEqualTo(sender); // Because we are sending response to the sender
		assertThat(result.getSender()).isEqualTo(sender);
		assertThat(result.getSenderName()).isEqualTo(sender);
		assertThat(result.getMessage()).isEqualTo(template);
		assertThat(result.getEmailHeaders()).isNotNull().hasSize(3);
		assertThat(result.getEmailHeaders().get(EmailHeader.MESSAGE_ID)).isNull();
		assertThat(result.getEmailHeaders().get(EmailHeader.IN_REPLY_TO)).isNotNull().hasSize(1).contains("someValue");
		assertThat(result.getEmailHeaders().get(EmailHeader.REFERENCES)).isNotNull().hasSize(1).contains("someValue");
		assertThat(result.getEmailHeaders().get(EmailHeader.AUTO_SUBMITTED)).isNotNull().hasSize(1).contains("auto-generated");
	}

	@Test
	void toCommunicationAttachmentDataEntity() throws SQLException {

		// Arrange
		final var attachmentData = "attachmentData".getBytes();
		final var blob = new SerialBlob(attachmentData);
		when(blobBuilderMock.createBlob(attachmentData)).thenReturn(blob);

		// Act
		final var result = emailReaderMapper.toCommunicationAttachmentDataEntity(attachmentData);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getFile()).isNotNull();
		assertThat(result.getFile()).isSameAs(blob);
	}
}

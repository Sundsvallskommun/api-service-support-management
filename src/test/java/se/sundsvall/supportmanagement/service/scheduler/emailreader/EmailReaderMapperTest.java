package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.Direction.INBOUND;

import java.sql.Blob;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;
import se.sundsvall.supportmanagement.service.util.BlobBuilder;

import generated.se.sundsvall.emailreader.Email;
import generated.se.sundsvall.emailreader.EmailAttachment;

@ExtendWith(MockitoExtension.class)
class EmailReaderMapperTest {

	@Mock
	private BlobBuilder blobBuilder;

	@Mock
	private Blob blobMock;

	@InjectMocks
	private EmailReaderMapper emailReaderMapper;

	@Test
	void toAttachments() {

		when(blobBuilder.createBlob(anyString())).thenReturn(blobMock);

		final var email = new Email()
			.id("someId")
			.subject("someSubject")
			.recipients(List.of("someRecipient"))
			.sender("someSender")
			.message("someMessage")
			.receivedAt(OffsetDateTime.now())
			.attachments(List.of(new EmailAttachment()
				.name("someName")
				.content("someContent")
				.contentType("text/plain")
			));

		final var result = emailReaderMapper.toAttachments(email).getFirst();

		assertThat(result).isNotNull();
		assertThat(result.getFileName()).isEqualTo("someName");
		assertThat(result.getAttachmentData().getFile()).isSameAs(blobMock);
		assertThat(result.getMimeType()).isEqualTo("text/plain");
		assertThat(result.getErrandEntity()).isNull();
	}

	@Test
	void toAttachments_null() {

		final var result = emailReaderMapper.toAttachments(null);

		assertThat(result).isNotNull().isEmpty();
	}

	@Test
	void toCommunicationEntity() {

		when(blobBuilder.createBlob(anyString())).thenReturn(blobMock);

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
				.content("someContent")
				.contentType("text/plain")
			));

		final var result = emailReaderMapper.toCommunicationEntity(email);

		assertThat(result).isNotNull();
		assertThat(result.getSubject()).isEqualTo("someSubject");
		assertThat(result.getExternalCaseID()).isEmpty();
		assertThat(result.getErrandNumber()).isNull();
		assertThat(result.getSent()).isNotNull().isCloseTo(OffsetDateTime.now(), org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.SECONDS));
		assertThat(result.getDirection()).isEqualTo(INBOUND);
		assertThat(result.getType()).isEqualTo(CommunicationType.EMAIL);
		assertThat(result.getTarget()).isEqualTo("someSender");
		assertThat(result.getAttachments()).isNotNull().hasSize(1);
		assertThat(result.getAttachments().getFirst().getName()).isEqualTo("someName");
		assertThat(result.getAttachments().getFirst().getContentType()).isEqualTo("text/plain");
		assertThat(result.getAttachments().getFirst().getAttachmentData().getFile()).isNotNull().isEqualTo(blobMock);
		assertThat(result.getEmailHeaders()).isNotNull().hasSize(1);
		assertThat(result.getEmailHeaders().getFirst().getHeader()).isEqualTo(EmailHeader.MESSAGE_ID);
		assertThat(result.getEmailHeaders().getFirst().getValues()).isNotNull().hasSize(1).contains("someValue");
	}

	@Test
	void toCommunicationEntity_null() {

		final var result = emailReaderMapper.toCommunicationEntity(null);

		assertThat(result).isNull();
	}


	@Test
	void toErrand() {

		final var email = new Email()
			.id("someId")
			.subject("someSubject")
			.recipients(List.of("someRecipient"))
			.sender("someSender")
			.message("someMessage")
			.receivedAt(OffsetDateTime.now())
			.metadata(Map.of("classification.category", "someCategory", "classification.type", "someType"))
			.attachments(null);

		final var result = emailReaderMapper.toErrand(email);

		assertThat(result).isNotNull();
		assertThat(result.getTitle()).isEqualTo("someSubject");
		assertThat(result.getDescription()).isEqualTo("someMessage");
		assertThat(result.getStatus()).isEqualTo("NEW");
		assertThat(result.getClassification()).isNotNull();
		assertThat(result.getClassification().getCategory()).isEqualTo("someCategory");
		assertThat(result.getClassification().getType()).isEqualTo("someType");
		assertThat(result.getStakeholders()).isNotNull().hasSize(1);
		assertThat(result.getStakeholders().getFirst().getContactChannels()).isNotNull().hasSize(1);
		assertThat(result.getStakeholders().getFirst().getContactChannels().getFirst().getType()).isEqualTo("EMAIL");
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
		final Map<String, List<String>> emailHeaders = Map.of("MESSAGE_ID", List.of("someValue", "someOtherValue"));

		final var email = new Email()
			.subject(subject)
			.recipients(recipients)
			.sender(sender)
			.message(message)
			.headers(emailHeaders)
			.receivedAt(receivedAt);
		// Act
		final var result = emailReaderMapper.createEmailRequest(email, sender, template);

		// Assert
		assertThat(result).isNotNull().hasNoNullFieldsOrPropertiesExcept("senderName", "htmlMessage", "attachments");
		assertThat(result.getMessage()).isEqualTo(template);
		assertThat(result.getSubject()).isEqualTo(subject);
		assertThat(result.getRecipient()).isEqualTo(sender); // Because we are sending response to the sender
		assertThat(result.getSender()).isEqualTo(sender);
		assertThat(result.getMessage()).isEqualTo(template);

	}


}

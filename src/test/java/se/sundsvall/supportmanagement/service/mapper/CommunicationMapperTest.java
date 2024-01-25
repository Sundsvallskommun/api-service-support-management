package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.sql.Blob;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.supportmanagement.api.model.communication.Communication;
import se.sundsvall.supportmanagement.api.model.communication.CommunicationAttachment;
import se.sundsvall.supportmanagement.api.model.communication.EmailAttachment;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEmailHeaderEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;
import se.sundsvall.supportmanagement.service.util.BlobBuilder;


@ExtendWith(MockitoExtension.class)
class CommunicationMapperTest {

	@Mock
	private BlobBuilder blobBuilder;

	@Mock
	private Blob blobMock;


	@InjectMocks
	private CommunicationMapper communicationMapper;

	@Test
	void toCommunicationsReturnsCorrectCommunicationList() {
		final var entities = singletonList(createCommunicationEntity());
		final var communications = communicationMapper.toCommunications(entities);

		assertThat(communications).hasSize(1);
		assertCommunicationMatchesEntity(communications.getFirst(), entities.getFirst());
	}

	@Test
	void toCommunicationReturnsCorrectCommunication() {
		final var entity = createCommunicationEntity();
		final var communication = communicationMapper.toCommunication(entity);

		assertCommunicationMatchesEntity(communication, entity);
	}

	@Test
	void toCommmunicationReturnsNullWhenInputIsNull() {
		final var communications = communicationMapper.toCommunication(null);

		assertThat(communications).isNull();
	}

	@Test
	void toAttachmentsReturnsCorrectAttachmentList() {
		final var entities = singletonList(createCommunicationAttachmentEntity());
		final var attachments = communicationMapper.toAttachments(entities);

		assertThat(attachments).hasSize(1);
		assertAttachmentMatchesEntity(attachments.getFirst(), entities.getFirst());
	}

	@Test
	void toAttachmentReturnsCorrectAttachment() {
		final var entity = createCommunicationAttachmentEntity();
		final var attachment = communicationMapper.toAttachment(entity);

		assertAttachmentMatchesEntity(attachment, entity);
	}

	@Test
	void toCommunicationsReturnsEmptyListWhenInputIsEmpty() {
		final var communications = communicationMapper.toCommunications(Collections.emptyList());

		assertThat(communications).isEmpty();
	}

	@Test
	void toAttachmentsReturnsEmptyListWhenInputIsEmpty() {
		final var attachments = communicationMapper.toAttachments(Collections.emptyList());

		assertThat(attachments).isEmpty();
	}


	@Test
	void toCommunicationEntityFromEmailRequest() {

		when(blobBuilder.createBlob(anyString())).thenReturn(blobMock);

		final var emailRequest = new EmailRequest()
			.withRecipient("recipient")
			.withSender("sender")
			.withSenderName("senderName")
			.withSubject("subject")
			.withMessage("message")
			.withHtmlMessage("htmlMessage")
			.withEmailHeaders(Map.of(EmailHeader.MESSAGE_ID, List.of("<test@test.se>")))
			.withAttachments(singletonList(new EmailAttachment().withName("name").withBase64EncodedString("base64EncodedString")));

		final var communicationEntity = communicationMapper.toCommunicationEntity(emailRequest);

		assertThat(communicationEntity).isNotNull().hasNoNullFieldsOrPropertiesExcept("errandNumber", "externalCaseID");
		assertThat(testValidUUID(communicationEntity.getId())).isTrue();
		assertThat(communicationEntity.getDirection()).isEqualTo(Direction.OUTBOUND);
		assertThat(communicationEntity.getTarget()).isEqualTo(emailRequest.getRecipient());
		assertThat(communicationEntity.getType()).isEqualTo(CommunicationType.EMAIL);
		assertThat(communicationEntity.getSubject()).isEqualTo(emailRequest.getSubject());
		assertThat(communicationEntity.getMessageBody()).isEqualTo(emailRequest.getMessage());
		assertThat(communicationEntity.getAttachments()).hasSize(1);
		assertThat(communicationEntity.getAttachments().getFirst().getName()).isEqualTo("name");
		assertThat(communicationEntity.getAttachments().getFirst().getContentType()).isEqualTo("application/octet-stream");
		assertThat(communicationEntity.getAttachments().getFirst().getAttachmentData().getFile()).isEqualTo(blobMock);
	}


	@Test
	void toCommunicationEntityFromSmsRequest() {
		final var smsRequest = new SmsRequest()
			.withRecipient("recipient")
			.withSender("sender")
			.withMessage("message");

		final var communicationEntity = communicationMapper.toCommunicationEntity(smsRequest);

		assertThat(communicationEntity).isNotNull().hasNoNullFieldsOrPropertiesExcept("errandNumber", "externalCaseID", "subject", "attachments", "emailHeaders");
		assertThat(testValidUUID(communicationEntity.getId())).isTrue();
		assertThat(communicationEntity.getDirection()).isEqualTo(Direction.OUTBOUND);
		assertThat(communicationEntity.getTarget()).isEqualTo(smsRequest.getRecipient());
		assertThat(communicationEntity.getType()).isEqualTo(CommunicationType.SMS);
		assertThat(communicationEntity.getMessageBody()).isEqualTo(smsRequest.getMessage());
	}


	@SuppressWarnings("ResultOfMethodCallIgnored")
	private boolean testValidUUID(final String id) {

		try {
			UUID.fromString(id);
			return true;
			// If no exception is thrown, then the ID is a valid UUID.
		} catch (final IllegalArgumentException e) {
			Assertions.fail("ID is not a valid UUID");

		}
		return false;
	}

	private CommunicationEntity createCommunicationEntity() {
		final CommunicationEntity entity = new CommunicationEntity();
		entity.setId("testid");
		entity.setErrandNumber("testErrandNumber");
		entity.setDirection(Direction.INBOUND);
		entity.setMessageBody("testMessageBody");
		entity.setSent(OffsetDateTime.now());
		entity.setSubject("testSubject");
		entity.setType(CommunicationType.EMAIL);
		entity.setTarget("target");
		entity.setViewed(true);
		entity.setEmailHeaders(Collections.singletonList(CommunicationEmailHeaderEntity.create().withHeader(EmailHeader.IN_REPLY_TO).withValues(Collections.singletonList("someValue"))));
		entity.setAttachments(Collections.singletonList(createCommunicationAttachmentEntity()));
		return entity;
	}

	private CommunicationAttachmentEntity createCommunicationAttachmentEntity() {
		final CommunicationAttachmentEntity entity = new CommunicationAttachmentEntity();
		entity.setId("testId");
		entity.setName("testName");
		entity.setContentType("testContentType");
		return entity;
	}

	private void assertCommunicationMatchesEntity(final Communication communication, final CommunicationEntity entity) {
		assertThat(communication.getCommunicationID()).isEqualTo(entity.getId());
		assertThat(communication.getErrandNumber()).isEqualTo(entity.getErrandNumber());
		assertThat(communication.getDirection()).isEqualTo(entity.getDirection());
		assertThat(communication.getMessageBody()).isEqualTo(entity.getMessageBody());
		assertThat(communication.getSent()).isEqualTo(entity.getSent());
		assertThat(communication.getSubject()).isEqualTo(entity.getSubject());
		assertThat(communication.getCommunicationType()).isEqualTo(entity.getType());
		assertThat(communication.getTarget()).isEqualTo(entity.getTarget());
		assertThat(communication.isViewed()).isEqualTo(entity.isViewed());
		assertThat(communication.getCommunicationAttachments()).hasSize(1);
		assertAttachmentMatchesEntity(communication.getCommunicationAttachments().getFirst(), entity.getAttachments().getFirst());
		assertEmailHeadersMatchesEntity(communication.getEmailHeaders(), entity.getEmailHeaders());
	}

	private void assertEmailHeadersMatchesEntity(final Map<EmailHeader, List<String>> emailHeaders, final List<CommunicationEmailHeaderEntity> emailHeaderEntities) {
		assertThat(emailHeaders).hasSize(1);
		assertThat(emailHeaders.get(EmailHeader.IN_REPLY_TO)).hasSize(1);
		assertThat(emailHeaders.get(EmailHeader.IN_REPLY_TO).getFirst()).isEqualTo(emailHeaderEntities.getFirst().getValues().getFirst());

	}

	private void assertAttachmentMatchesEntity(final CommunicationAttachment attachment, final CommunicationAttachmentEntity entity) {
		assertThat(attachment.getAttachmentID()).isEqualTo(entity.getId());
		assertThat(attachment.getName()).isEqualTo(entity.getName());
		assertThat(attachment.getContentType()).isEqualTo(entity.getContentType());
	}

}

package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.Blob;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import se.sundsvall.supportmanagement.api.model.communication.WebMessageAttachment;
import se.sundsvall.supportmanagement.api.model.communication.WebMessageRequest;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEmailHeaderEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;
import se.sundsvall.supportmanagement.service.util.BlobBuilder;

@ExtendWith(MockitoExtension.class)
class CommunicationMapperTest {

	private static final String NAMESPACE = "name.space";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_NUMBER = "errandNumber";

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
	void toCommunicationReturnsNullWhenInputIsNull() {
		final var communications = communicationMapper.toCommunication(null);

		assertThat(communications).isNull();
	}

	@Test
	void toCommunicationAttachmentsReturnsCorrectAttachmentList() {
		final var communicationAttachments = singletonList(createCommunicationAttachmentEntity());
		final var attachments = communicationMapper.toCommunicationAttachments(communicationAttachments, Collections.emptyList());

		assertThat(attachments).hasSize(1);
		assertAttachmentMatchesEntity(attachments.getFirst(), communicationAttachments.getFirst());
	}

	@Test
	void toAttachmentReturnsCorrectAttachment() {
		final var entity = createCommunicationAttachmentEntity();
		final var attachment = communicationMapper.toAttachment(entity);

		assertAttachmentMatchesEntity(attachment, entity);
	}

	@Test
	void toAttachments() {

		final var entity = createCommunicationEntity();
		final var attachments = communicationMapper.toAttachments(entity);

		assertThat(attachments).hasSize(1);
		assertThat(attachments.getFirst().getFileName()).isEqualTo(entity.getAttachments().getFirst().getName());
		assertThat(attachments.getFirst().getMimeType()).isEqualTo("application/octet-stream");
		assertThat(attachments.getFirst().getNamespace()).isEqualTo(NAMESPACE);
		assertThat(attachments.getFirst().getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(attachments.getFirst().getId()).isNull();
		assertThat(attachments.getFirst().getAttachmentData().getFile()).isEqualTo(entity.getAttachments().getFirst().getAttachmentData().getFile());
	}

	@Test
	void toCommunicationsReturnsEmptyListWhenInputIsEmpty() {
		final var communications = communicationMapper.toCommunications(Collections.emptyList());

		assertThat(communications).isEmpty();
	}

	@Test
	void toCommunicationAttachmentsReturnsEmptyListWhenInputIsEmpty() {
		final var attachments = communicationMapper.toCommunicationAttachments(Collections.emptyList(), Collections.emptyList());

		assertThat(attachments).isEmpty();
	}

	@Test
	void toCommunicationEntityFromEmailRequest() {

		when(blobBuilder.createBlob(any())).thenReturn(blobMock);

		final var emailRequest = new EmailRequest()
			.withRecipient("recipient")
			.withSender("sender")
			.withSenderName("senderName")
			.withSubject("subject")
			.withMessage("message")
			.withHtmlMessage("htmlMessage")
			.withInternal(true)
			.withEmailHeaders(Map.of(EmailHeader.MESSAGE_ID, List.of("<test@test.se>")))
			.withAttachments(singletonList(new EmailAttachment().withFileName("name").withBase64EncodedString("base64EncodedString")));

		final var communicationEntity = communicationMapper.toCommunicationEntity(NAMESPACE, MUNICIPALITY_ID, emailRequest);

		assertThat(communicationEntity).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "errandNumber", "externalId", "errandAttachments", "senderUserId");
		assertThat(communicationEntity.getSender()).isEqualTo(emailRequest.getSender());
		assertThat(communicationEntity.getDirection()).isEqualTo(Direction.OUTBOUND);
		assertThat(communicationEntity.getTarget()).isEqualTo(emailRequest.getRecipient());
		assertThat(communicationEntity.getType()).isEqualTo(CommunicationType.EMAIL);
		assertThat(communicationEntity.getSubject()).isEqualTo(emailRequest.getSubject());
		assertThat(communicationEntity.getMessageBody()).isEqualTo(emailRequest.getMessage());
		assertThat(communicationEntity.getAttachments()).hasSize(1);
		assertThat(communicationEntity.getAttachments().getFirst().getName()).isEqualTo("name");
		assertThat(communicationEntity.getAttachments().getFirst().getContentType()).isEqualTo("application/octet-stream");
		assertThat(communicationEntity.getAttachments().getFirst().getAttachmentData().getFile()).isSameAs(blobMock);
		assertThat(communicationEntity.isInternal()).isTrue();

	}

	@Test
	void toCommunicationEntityFromSmsRequest() {
		final var smsRequest = new SmsRequest()
			.withRecipient("recipient")
			.withSender("sender")
			.withInternal(true)
			.withMessage("message");

		final var communicationEntity = communicationMapper.toCommunicationEntity(NAMESPACE, MUNICIPALITY_ID, smsRequest);

		assertThat(communicationEntity).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "errandNumber", "externalId", "subject", "attachments", "emailHeaders", "errandAttachments", "senderUserId");
		assertThat(communicationEntity.getSender()).isEqualTo(smsRequest.getSender());
		assertThat(communicationEntity.getDirection()).isEqualTo(Direction.OUTBOUND);
		assertThat(communicationEntity.getTarget()).isEqualTo(smsRequest.getRecipient());
		assertThat(communicationEntity.getType()).isEqualTo(CommunicationType.SMS);
		assertThat(communicationEntity.getMessageBody()).isEqualTo(smsRequest.getMessage());
		assertThat(communicationEntity.isInternal()).isTrue();
	}

	@Test
	void toCommunicationEntityFromWebMessageRequest() {

		final var adUser = "adUser";
		final var fullName = "fullName";

		when(blobBuilder.createBlob(any())).thenReturn(blobMock);

		final var webMessageRequest = new WebMessageRequest()
			.withMessage("message")
			.withInternal(true)
			.withAttachments(List.of(new WebMessageAttachment().withFileName("name").withBase64EncodedString("base64EncodedString")));

		final var communicationEntity = communicationMapper.toCommunicationEntity(NAMESPACE, MUNICIPALITY_ID, ERRAND_NUMBER, webMessageRequest, fullName, adUser);

		assertThat(communicationEntity).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "externalId", "sender", "target", "subject", "errandAttachments", "emailHeaders");
		assertThat(communicationEntity.getErrandNumber()).isEqualTo(ERRAND_NUMBER);
		assertThat(communicationEntity.getDirection()).isEqualTo(Direction.OUTBOUND);
		assertThat(communicationEntity.getType()).isEqualTo(CommunicationType.WEB_MESSAGE);
		assertThat(communicationEntity.getSender()).isEqualTo(fullName);
		assertThat(communicationEntity.getSenderUserId()).isEqualTo(adUser);
		assertThat(communicationEntity.getMessageBody()).isEqualTo(webMessageRequest.getMessage());
		assertThat(communicationEntity.getAttachments()).hasSize(1);
		assertThat(communicationEntity.getAttachments().getFirst().getName()).isEqualTo("name");
		assertThat(communicationEntity.getAttachments().getFirst().getContentType()).isEqualTo("application/octet-stream");
		assertThat(communicationEntity.getAttachments().getFirst().getAttachmentData().getFile()).isSameAs(blobMock);
		assertThat(communicationEntity.isInternal()).isTrue();

	}

	private CommunicationEntity createCommunicationEntity() {
		return new CommunicationEntity()
			.withSender("testSender")
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandNumber("testErrandNumber")
			.withDirection(Direction.INBOUND)
			.withMessageBody("testMessageBody")
			.withSent(OffsetDateTime.now())
			.withSubject("testSubject")
			.withType(CommunicationType.EMAIL)
			.withTarget("target")
			.withViewed(true)
			.withInternal(true)
			.withEmailHeaders(Collections.singletonList(CommunicationEmailHeaderEntity.create().withHeader(EmailHeader.IN_REPLY_TO).withValues(Collections.singletonList("someValue"))))
			.withErrandAttachments(Collections.singletonList(createAttachmentEntity()))
			.withAttachments(Collections.singletonList(createCommunicationAttachmentEntity()));
	}

	private AttachmentEntity createAttachmentEntity() {
		return new AttachmentEntity()
			.withId("testId")
			.withFileName("testFileName")
			.withMimeType("testMimeType");
	}

	private CommunicationAttachmentEntity createCommunicationAttachmentEntity() {
		return new CommunicationAttachmentEntity()
			.withId("testId")
			.withName("testName")
			.withContentType("application/octet-stream")
			.withAttachmentData(new AttachmentDataEntity().withFile(blobMock));
	}

	private void assertCommunicationMatchesEntity(final Communication communication, final CommunicationEntity entity) {
		assertThat(communication).isNotNull().hasNoNullFieldsOrPropertiesExcept("communicationID");
		assertThat(communication.getCommunicationID()).isEqualTo(entity.getId());
		assertThat(communication.getSender()).isEqualTo(entity.getSender());
		assertThat(communication.getErrandNumber()).isEqualTo(entity.getErrandNumber());
		assertThat(communication.getDirection()).isEqualTo(entity.getDirection());
		assertThat(communication.getMessageBody()).isEqualTo(entity.getMessageBody());
		assertThat(communication.getSent()).isEqualTo(entity.getSent());
		assertThat(communication.getSubject()).isEqualTo(entity.getSubject());
		assertThat(communication.getCommunicationType()).isEqualTo(entity.getType());
		assertThat(communication.getTarget()).isEqualTo(entity.getTarget());
		assertThat(communication.getViewed()).isEqualTo(entity.isViewed());
		assertThat(communication.isInternal()).isEqualTo(entity.isInternal());
		assertThat(communication.getCommunicationAttachments()).hasSize(2);
		assertAttachmentMatchesEntity(communication.getCommunicationAttachments().getFirst(), entity.getAttachments().getFirst());
		assertEmailHeadersMatchesEntity(communication.getEmailHeaders(), entity.getEmailHeaders());
	}

	private void assertEmailHeadersMatchesEntity(final Map<EmailHeader, List<String>> emailHeaders, final List<CommunicationEmailHeaderEntity> emailHeaderEntities) {
		assertThat(emailHeaders).hasSize(1);
		assertThat(emailHeaders.get(EmailHeader.IN_REPLY_TO)).hasSize(1);
		assertThat(emailHeaders.get(EmailHeader.IN_REPLY_TO).getFirst()).isEqualTo(emailHeaderEntities.getFirst().getValues().getFirst());

	}

	private void assertAttachmentMatchesEntity(final CommunicationAttachment attachment, final CommunicationAttachmentEntity entity) {
		assertThat(attachment.getId()).isEqualTo(entity.getId());
		assertThat(attachment.getFileName()).isEqualTo(entity.getName());
		assertThat(attachment.getMimeType()).isEqualTo(entity.getContentType());
	}

}

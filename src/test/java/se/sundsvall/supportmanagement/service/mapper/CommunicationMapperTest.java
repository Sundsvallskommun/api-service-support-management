package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import se.sundsvall.supportmanagement.api.model.communication.Communication;
import se.sundsvall.supportmanagement.api.model.communication.CommunicationAttachment;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;

class CommunicationMapperTest {

	@Test
	void toCommunicationsReturnsCorrectCommunicationList() {
		final var entities = singletonList(createCommunicationEntity());
		final var communications = CommunicationMapper.toCommunications(entities);

		assertThat(communications).hasSize(1);
		assertCommunicationMatchesEntity(communications.getFirst(), entities.getFirst());
	}

	@Test
	void toCommunicationReturnsCorrectCommunication() {
		final var entity = createCommunicationEntity();
		final var communication = CommunicationMapper.toCommunication(entity);

		assertCommunicationMatchesEntity(communication, entity);
	}

	@Test
	void toAttachmentsReturnsCorrectAttachmentList() {
		final var entities = singletonList(createCommunicationAttachmentEntity());
		final var attachments = CommunicationMapper.toAttachments(entities);

		assertThat(attachments).hasSize(1);
		assertAttachmentMatchesEntity(attachments.getFirst(), entities.getFirst());
	}

	@Test
	void toAttachmentReturnsCorrectAttachment() {
		final var entity = createCommunicationAttachmentEntity();
		final var attachment = CommunicationMapper.toAttachment(entity);

		assertAttachmentMatchesEntity(attachment, entity);
	}

	@Test
	void toCommunicationsReturnsEmptyListWhenInputIsEmpty() {
		final var communications = CommunicationMapper.toCommunications(Collections.emptyList());

		assertThat(communications).isEmpty();
	}

	@Test
	void toAttachmentsReturnsEmptyListWhenInputIsEmpty() {
		final var attachments = CommunicationMapper.toAttachments(Collections.emptyList());

		assertThat(attachments).isEmpty();
	}

	private CommunicationEntity createCommunicationEntity() {
		final CommunicationEntity entity = new CommunicationEntity();
		entity.setCommunicationID("testID");
		entity.setErrandNumber("testErrandNumber");
		entity.setDirection(Direction.INBOUND);
		entity.setMessageBody("testMessageBody");
		entity.setSent(OffsetDateTime.now());
		entity.setSubject("testSubject");
		entity.setCommunicationType(CommunicationType.EMAIL);
		entity.setMobileNumber("1234567890");
		entity.setEmail("test@example.com");
		entity.setViewed(true);
		entity.setAttachments(Collections.singletonList(createCommunicationAttachmentEntity()));
		return entity;
	}

	private CommunicationAttachmentEntity createCommunicationAttachmentEntity() {
		final CommunicationAttachmentEntity entity = new CommunicationAttachmentEntity();
		entity.setAttachmentID("testAttachmentID");
		entity.setName("testName");
		entity.setContentType("testContentType");
		return entity;
	}

	private void assertCommunicationMatchesEntity(final Communication communication, final CommunicationEntity entity) {
		assertThat(communication.getCommunicationID()).isEqualTo(entity.getCommunicationID());
		assertThat(communication.getErrandNumber()).isEqualTo(entity.getErrandNumber());
		assertThat(communication.getDirection()).isEqualTo(entity.getDirection());
		assertThat(communication.getMessageBody()).isEqualTo(entity.getMessageBody());
		assertThat(communication.getSent()).isEqualTo(entity.getSent());
		assertThat(communication.getSubject()).isEqualTo(entity.getSubject());
		assertThat(communication.getCommunicationType()).isEqualTo(entity.getCommunicationType());
		assertThat(communication.getMobileNumber()).isEqualTo(entity.getMobileNumber());
		assertThat(communication.getEmail()).isEqualTo(entity.getEmail());
		assertThat(communication.isViewed()).isEqualTo(entity.isViewed());
		assertThat(communication.getCommunicationAttachments()).hasSize(1);
		assertAttachmentMatchesEntity(communication.getCommunicationAttachments().getFirst(), entity.getAttachments().getFirst());
	}

	private void assertAttachmentMatchesEntity(final CommunicationAttachment attachment, final CommunicationAttachmentEntity entity) {
		assertThat(attachment.getAttachmentID()).isEqualTo(entity.getAttachmentID());
		assertThat(attachment.getName()).isEqualTo(entity.getName());
		assertThat(attachment.getContentType()).isEqualTo(entity.getContentType());
	}

}

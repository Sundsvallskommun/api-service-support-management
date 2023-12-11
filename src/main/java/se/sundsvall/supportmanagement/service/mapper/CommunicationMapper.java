package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;

import java.util.List;

import se.sundsvall.supportmanagement.api.model.communication.Communication;
import se.sundsvall.supportmanagement.api.model.communication.CommunicationAttachment;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;

public final class CommunicationMapper {

	private CommunicationMapper() {
		// Intentionally empty
	}

	public static List<Communication> toCommunications(final List<CommunicationEntity> entities) {
		return entities.stream()
			.map(CommunicationMapper::toCommunication)
			.toList();
	}

	public static Communication toCommunication(final CommunicationEntity entity) {
		return Communication.create()
			.withCommunicationID(entity.getCommunicationID())
			.withErrandNumber(entity.getErrandNumber())
			.withDirection(entity.getDirection())
			.withMessageBody(entity.getMessageBody())
			.withSent(entity.getSent())
			.withSubject(entity.getSubject())
			.withCommunicationType(entity.getCommunicationType())
			.withMobileNumber(entity.getMobileNumber())
			.withEmail(entity.getEmail())
			.withViewed(entity.isViewed())
			.withCommunicationAttachments(toAttachments(entity.getAttachments()));
	}

	public static List<CommunicationAttachment> toAttachments(final List<CommunicationAttachmentEntity> attachments) {

		if (attachments == null) {
			return emptyList();
		}

		return attachments.stream()
			.map(CommunicationMapper::toAttachment)
			.toList();
	}

	public static CommunicationAttachment toAttachment(final CommunicationAttachmentEntity entity) {
		return CommunicationAttachment.create()
			.withAttachmentID(entity.getAttachmentID())
			.withName(entity.getName())
			.withContentType(entity.getContentType());
	}


}

package se.sundsvall.supportmanagement.service.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
		if (entity == null) {
			return null;
		}
		return Communication.create()
			.withCommunicationID(entity.getId())
			.withErrandNumber(entity.getErrandNumber())
			.withDirection(entity.getDirection())
			.withMessageBody(entity.getMessageBody())
			.withSent(entity.getSent())
			.withSubject(entity.getSubject())
			.withCommunicationType(entity.getType())
			.withTarget(entity.getTarget())
			.withViewed(entity.isViewed())
			.withCommunicationAttachments(toAttachments(entity.getAttachments()));
	}

	public static List<CommunicationAttachment> toAttachments(final List<CommunicationAttachmentEntity> attachments) {

		return Optional.ofNullable(attachments)
			.orElse(Collections.emptyList())
			.stream()
			.map(CommunicationMapper::toAttachment)
			.toList();
	}

	public static CommunicationAttachment toAttachment(final CommunicationAttachmentEntity entity) {
		return CommunicationAttachment.create()
			.withAttachmentID(entity.getId())
			.withName(entity.getName())
			.withContentType(entity.getContentType());
	}


}

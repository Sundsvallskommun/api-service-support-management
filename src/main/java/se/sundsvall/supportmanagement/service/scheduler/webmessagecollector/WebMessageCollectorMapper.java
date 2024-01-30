package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;

import generated.se.sundsvall.webmessagecollector.MessageDTO;

public final class WebMessageCollectorMapper {

	private WebMessageCollectorMapper() {
		// Intentionally left empty
	}

	static CommunicationEntity toCommunicationEntity(final MessageDTO messageDTO, final String errandNumber) {
		return CommunicationEntity.create()
			.withId(UUID.randomUUID().toString())
			.withDirection(Direction.INBOUND)
			.withErrandNumber(errandNumber)
			.withExternalCaseID(messageDTO.getExternalCaseId())
			.withMessageBody(messageDTO.getMessage())
			.withSent(OffsetDateTime.of(LocalDateTime.parse(messageDTO.getSent()), OffsetDateTime.now().getOffset()))
			.withType(CommunicationType.EMAIL)
			.withViewed(false);
	}

}

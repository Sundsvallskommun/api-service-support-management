package se.sundsvall.supportmanagement.service.mapper;

import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.api.model.config.MessageExchangeSync;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeSyncEntity;

@Component
public class MessageExchangeSyncMapper {
	public MessageExchangeSyncEntity toEntity(MessageExchangeSync config, String municipalityId) {
		return MessageExchangeSyncEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(config.getNamespace())
			.withActive(config.getActive())
			.withLatestSyncedSequenceNumber(config.getLatestSyncedSequenceNumber());
	}

	public MessageExchangeSync toMessageExchangeSync(MessageExchangeSyncEntity entity) {
		return MessageExchangeSync.create()
			.withId(entity.getId())
			.withNamespace(entity.getNamespace())
			.withLatestSyncedSequenceNumber(entity.getLatestSyncedSequenceNumber())
			.withModified(entity.getModified())
			.withActive(entity.isActive());

	}
}

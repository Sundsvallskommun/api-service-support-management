package se.sundsvall.supportmanagement.service.mapper;

import se.sundsvall.supportmanagement.api.model.config.MessageExchangeIntegration;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeIntegrationConfigEntity;

public final class MessageExchangeIntegrationMapper {

	private MessageExchangeIntegrationMapper() {}

	public static MessageExchangeIntegrationConfigEntity toEntity(final MessageExchangeIntegration config, final String namespace, final String municipalityId) {
		return MessageExchangeIntegrationConfigEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withTriggerStatusChangeOn(config.getTriggerStatusChangeOn())
			.withStatusChangeTo(config.getStatusChangeTo());
	}

	public static MessageExchangeIntegration toMessageExchangeIntegration(final MessageExchangeIntegrationConfigEntity entity) {
		return MessageExchangeIntegration.create()
			.withTriggerStatusChangeOn(entity.getTriggerStatusChangeOn())
			.withStatusChangeTo(entity.getStatusChangeTo())
			.withCreated(entity.getCreated())
			.withModified(entity.getModified());
	}
}

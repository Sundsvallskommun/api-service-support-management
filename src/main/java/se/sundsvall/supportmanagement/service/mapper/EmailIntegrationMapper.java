package se.sundsvall.supportmanagement.service.mapper;

import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.api.model.config.EmailIntegration;
import se.sundsvall.supportmanagement.integration.db.model.EmailWorkerConfigEntity;

@Component
public class EmailIntegrationMapper {

	public EmailWorkerConfigEntity toEntity(final EmailIntegration config, final String namespace, final String municipalityId) {
		return EmailWorkerConfigEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withEnabled(config.getEnabled())
			.withErrandClosedEmailSender(config.getErrandClosedEmailSender())
			.withErrandClosedEmailTemplate(config.getErrandClosedEmailTemplate())
			.withErrandClosedEmailHTMLTemplate(config.getErrandClosedEmailHTMLTemplate())
			.withErrandNewEmailSender(config.getErrandNewEmailSender())
			.withErrandNewEmailTemplate(config.getErrandNewEmailTemplate())
			.withErrandNewEmailHTMLTemplate(config.getErrandNewEmailHTMLTemplate())
			.withDaysOfInactivityBeforeReject(config.getDaysOfInactivityBeforeReject())
			.withStatusForNew(config.getStatusForNew())
			.withTriggerStatusChangeOn(config.getTriggerStatusChangeOn())
			.withStatusChangeTo(config.getStatusChangeTo())
			.withInactiveStatus(config.getInactiveStatus())
			.withAddSenderAsStakeholder(config.getAddSenderAsStakeholder() != null && config.getAddSenderAsStakeholder())
			.withErrandChannel(config.getErrandChannel())
			.withStakeholderRole(config.getStakeholderRole());
	}

	public EmailIntegration toEmailIntegration(final EmailWorkerConfigEntity entity) {
		return EmailIntegration.create()
			.withEnabled(entity.getEnabled())
			.withErrandClosedEmailSender(entity.getErrandClosedEmailSender())
			.withErrandClosedEmailTemplate(entity.getErrandClosedEmailTemplate())
			.withErrandClosedEmailHTMLTemplate(entity.getErrandClosedEmailHTMLTemplate())
			.withErrandNewEmailSender(entity.getErrandNewEmailSender())
			.withErrandNewEmailTemplate(entity.getErrandNewEmailTemplate())
			.withErrandNewEmailHTMLTemplate(entity.getErrandNewEmailHTMLTemplate())
			.withDaysOfInactivityBeforeReject(entity.getDaysOfInactivityBeforeReject())
			.withStatusForNew(entity.getStatusForNew())
			.withTriggerStatusChangeOn(entity.getTriggerStatusChangeOn())
			.withStatusChangeTo(entity.getStatusChangeTo())
			.withInactiveStatus(entity.getInactiveStatus())
			.withAddSenderAsStakeholder(entity.isAddSenderAsStakeholder())
			.withStakeholderRole(entity.getStakeholderRole())
			.withErrandChannel(entity.getErrandChannel())
			.withCreated(entity.getCreated())
			.withModified(entity.getModified());
	}
}

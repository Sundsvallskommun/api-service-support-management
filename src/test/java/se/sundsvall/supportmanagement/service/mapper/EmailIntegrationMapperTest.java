package se.sundsvall.supportmanagement.service.mapper;


import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.config.EmailIntegration;
import se.sundsvall.supportmanagement.integration.db.model.EmailWorkerConfigEntity;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EmailIntegrationMapperTest {

	private EmailIntegrationMapper mapper = new EmailIntegrationMapper();

	@Test
	void toEntity() {

		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var enabled = true;
		final var errandClosedEmailSender = "errandClosedEmailSender";
		final var errandClosedEmailTemplate = "errandClosedEmailTemplate";
		final var errandNewEmailSender = "errandNewEmailSender";
		final var errandNewEmailTemplate = "errandNewEmailTemplate";
		final var daysOfInactivityBeforeReject = 5;
		final var statusForNew = "statusForNew";
		final var triggerStatusChangeOn = "triggerStatusChangeOn";
		final var statusChangeTo = "statusChangeTo";
		final var inactiveStatus = "inactiveStatus";
		final var addSenderAsStakeholder = true;
		final var stakeholderRole = "stakeholderRole";
		final var errandChannel = "errandChannel";
		final var created = OffsetDateTime.now();
		final var modified = OffsetDateTime.now().plusDays(1);

		final var config = EmailIntegration.create()
			.withEnabled(enabled)
			.withErrandClosedEmailSender(errandClosedEmailSender)
			.withErrandClosedEmailTemplate(errandClosedEmailTemplate)
			.withErrandNewEmailSender(errandNewEmailSender)
			.withErrandNewEmailTemplate(errandNewEmailTemplate)
			.withDaysOfInactivityBeforeReject(daysOfInactivityBeforeReject)
			.withStatusForNew(statusForNew)
			.withTriggerStatusChangeOn(triggerStatusChangeOn)
			.withStatusChangeTo(statusChangeTo)
			.withInactiveStatus(inactiveStatus)
			.withAddSenderAsStakeholder(addSenderAsStakeholder)
			.withStakeholderRole(stakeholderRole)
			.withErrandChannel(errandChannel)
			.withCreated(created)
			.withModified(modified);


		var entity = mapper.toEntity(config, namespace, municipalityId);

		assertThat(config).hasNoNullFieldsOrPropertiesExcept("id", "created", "modified");
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getEnabled()).isEqualTo(enabled);
		assertThat(entity.getErrandClosedEmailSender()).isEqualTo(errandClosedEmailSender);
		assertThat(entity.getErrandClosedEmailTemplate()).isEqualTo(errandClosedEmailTemplate);
		assertThat(entity.getErrandNewEmailSender()).isEqualTo(errandNewEmailSender);
		assertThat(entity.getErrandNewEmailTemplate()).isEqualTo(errandNewEmailTemplate);
		assertThat(entity.getDaysOfInactivityBeforeReject()).isEqualTo(daysOfInactivityBeforeReject);
		assertThat(entity.getStatusForNew()).isEqualTo(statusForNew);
		assertThat(entity.getTriggerStatusChangeOn()).isEqualTo(triggerStatusChangeOn);
		assertThat(entity.getStatusChangeTo()).isEqualTo(statusChangeTo);
		assertThat(entity.getInactiveStatus()).isEqualTo(inactiveStatus);
		assertThat(entity.isAddSenderAsStakeholder()).isEqualTo(addSenderAsStakeholder);
		assertThat(entity.getStakeholderRole()).isEqualTo(stakeholderRole);
		assertThat(entity.getErrandChannel()).isEqualTo(errandChannel);
	}

	@Test
	void toEmailIntegration() {
		final var id = 1L;
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var enabled = true;
		final var errandClosedEmailSender = "noreply@email.se";
		final var errandClosedEmailTemplate = "This is an email";
		final var errandNewEmailSender = "test@email.se";
		final var errandNewEmailTemplate = "This is an email too";
		final var daysOfInactivityBeforeReject = 3;
		final var statusForNew = "NEW";
		final var triggerStatusChangeOn = "SOLVED";
		final var statusChangeTo = "OPEN";
		final var inactiveStatus = "CLOSED";
		final var addSenderAsStakeholder = true;
		final var stakeholderRole = "stakeholderRole";
		final var errandChannel = "errandChannel";
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();

		final var entity = EmailWorkerConfigEntity.create()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withEnabled(enabled)
			.withErrandClosedEmailSender(errandClosedEmailSender)
			.withErrandClosedEmailTemplate(errandClosedEmailTemplate)
			.withErrandNewEmailSender(errandNewEmailSender)
			.withErrandNewEmailTemplate(errandNewEmailTemplate)
			.withDaysOfInactivityBeforeReject(daysOfInactivityBeforeReject)
			.withStatusForNew(statusForNew)
			.withTriggerStatusChangeOn(triggerStatusChangeOn)
			.withStatusChangeTo(statusChangeTo)
			.withInactiveStatus(inactiveStatus)
			.withAddSenderAsStakeholder(addSenderAsStakeholder)
			.withStakeholderRole(stakeholderRole)
			.withErrandChannel(errandChannel)
			.withCreated(created)
			.withModified(modified);

		var config = mapper.toEmailIntegration(entity);

		assertThat(config).hasNoNullFieldsOrProperties();
		assertThat(config.getEnabled()).isEqualTo(enabled);
		assertThat(config.getErrandClosedEmailSender()).isEqualTo(errandClosedEmailSender);
		assertThat(config.getErrandClosedEmailTemplate()).isEqualTo(errandClosedEmailTemplate);
		assertThat(config.getErrandNewEmailSender()).isEqualTo(errandNewEmailSender);
		assertThat(config.getErrandNewEmailTemplate()).isEqualTo(errandNewEmailTemplate);
		assertThat(config.getDaysOfInactivityBeforeReject()).isEqualTo(daysOfInactivityBeforeReject);
		assertThat(config.getStatusForNew()).isEqualTo(statusForNew);
		assertThat(config.getTriggerStatusChangeOn()).isEqualTo(triggerStatusChangeOn);
		assertThat(config.getStatusChangeTo()).isEqualTo(statusChangeTo);
		assertThat(config.getInactiveStatus()).isEqualTo(inactiveStatus);
		assertThat(config.getAddSenderAsStakeholder()).isEqualTo(addSenderAsStakeholder);
		assertThat(config.getStakeholderRole()).isEqualTo(stakeholderRole);
		assertThat(config.getErrandChannel()).isEqualTo(errandChannel);
		assertThat(config.getCreated()).isEqualTo(created);
		assertThat(config.getModified()).isEqualTo(modified);
	}
}
package se.sundsvall.supportmanagement.service.mapper;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.config.MessageExchangeIntegration;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeIntegrationConfigEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.service.mapper.MessageExchangeIntegrationMapper.toEntity;
import static se.sundsvall.supportmanagement.service.mapper.MessageExchangeIntegrationMapper.toMessageExchangeIntegration;

class MessageExchangeIntegrationMapperTest {

	@Test
	void toEntityMaps() {
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var triggerStatusChangeOn = "SOLVED";
		final var statusChangeTo = "OPEN";

		final var config = MessageExchangeIntegration.create()
			.withTriggerStatusChangeOn(triggerStatusChangeOn)
			.withStatusChangeTo(statusChangeTo);

		final var entity = toEntity(config, namespace, municipalityId);

		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getTriggerStatusChangeOn()).isEqualTo(triggerStatusChangeOn);
		assertThat(entity.getStatusChangeTo()).isEqualTo(statusChangeTo);
		assertThat(entity.getId()).isNull();
		assertThat(entity.getCreated()).isNull();
		assertThat(entity.getModified()).isNull();
	}

	@Test
	void toMessageExchangeIntegrationMaps() {
		final var id = 1L;
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var triggerStatusChangeOn = "SOLVED";
		final var statusChangeTo = "OPEN";
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();

		final var entity = MessageExchangeIntegrationConfigEntity.create()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withTriggerStatusChangeOn(triggerStatusChangeOn)
			.withStatusChangeTo(statusChangeTo)
			.withCreated(created)
			.withModified(modified);

		final var config = toMessageExchangeIntegration(entity);

		assertThat(config).hasNoNullFieldsOrProperties();
		assertThat(config.getTriggerStatusChangeOn()).isEqualTo(triggerStatusChangeOn);
		assertThat(config.getStatusChangeTo()).isEqualTo(statusChangeTo);
		assertThat(config.getCreated()).isEqualTo(created);
		assertThat(config.getModified()).isEqualTo(modified);
	}
}

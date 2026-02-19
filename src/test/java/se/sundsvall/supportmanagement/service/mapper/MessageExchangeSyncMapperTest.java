package se.sundsvall.supportmanagement.service.mapper;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.config.MessageExchangeSync;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeSyncEntity;

import static org.assertj.core.api.Assertions.assertThat;

class MessageExchangeSyncMapperTest {

	private final MessageExchangeSyncMapper mapper = new MessageExchangeSyncMapper();

	@Test
	void toEntity() {
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var active = true;
		final var latestSyncedSequenceNumber = 12L;

		final var config = MessageExchangeSync.create()
			.withId(33L)
			.withNamespace(namespace)
			.withModified(OffsetDateTime.now())
			.withLatestSyncedSequenceNumber(latestSyncedSequenceNumber)
			.withActive(active);

		final var entity = mapper.toEntity(config, municipalityId);

		assertThat(entity).hasNoNullFieldsOrPropertiesExcept("id", "modified");
		assertThat(entity.getId()).isNull();
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getModified()).isNull();
		assertThat(entity.getLatestSyncedSequenceNumber()).isEqualTo(latestSyncedSequenceNumber);
		assertThat(entity.isActive()).isEqualTo(active);
	}

	@Test
	void toMessageExchangeSync() {
		final var id = 33L;
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var active = true;
		final var latestSyncedSequenceNumber = 12L;
		final var modified = OffsetDateTime.now();

		final var entity = MessageExchangeSyncEntity.create()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withActive(active)
			.withLatestSyncedSequenceNumber(latestSyncedSequenceNumber)
			.withModified(modified);

		final var config = mapper.toMessageExchangeSync(entity);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(config).hasNoNullFieldsOrProperties();
		assertThat(config.getId()).isEqualTo(id);
		assertThat(config.getNamespace()).isEqualTo(namespace);
		assertThat(config.getActive()).isEqualTo(active);
		assertThat(config.getLatestSyncedSequenceNumber()).isEqualTo(latestSyncedSequenceNumber);
		assertThat(config.getModified()).isEqualTo(modified);
	}
}

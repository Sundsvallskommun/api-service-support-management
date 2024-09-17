package se.sundsvall.supportmanagement.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;

class NamespaceConfigMapperTest {

	private final NamespaceConfigMapper mapper = new NamespaceConfigMapper();

	@Test
	void toEntity() {
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var shortCode = "shortCode";

		final var config = NamespaceConfig.create().withShortCode(shortCode);

		final var entity = mapper.toEntity(config, namespace, municipalityId);

		assertThat(entity).hasNoNullFieldsOrPropertiesExcept("id", "created", "modified");
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getShortCode()).isEqualTo(shortCode);
	}

	@Test
	void toNamespaceConfig() {

		final var shortCode = "shortCode";
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();

		final var entity = NamespaceConfigEntity.create()
			.withShortCode(shortCode)
			.withCreated(created)
			.withModified(modified);

		final var config = mapper.toNamespaceConfig(entity);

		assertThat(config).hasNoNullFieldsOrPropertiesExcept("namespace", "municipalityId", "displayName"); // TODO: This will be changed in UF-10005 where DB is extended to store displayname
		assertThat(config.getShortCode()).isEqualTo(shortCode);
		assertThat(config.getCreated()).isEqualTo(created);
		assertThat(config.getModified()).isEqualTo(modified);

	}
}

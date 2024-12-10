package se.sundsvall.supportmanagement.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
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
		final var displayName = "displayName";

		final var config = NamespaceConfig.create()
			.withDisplayName(displayName)
			.withShortCode(shortCode);

		final var entity = mapper.toEntity(config, namespace, municipalityId);

		assertThat(entity).hasNoNullFieldsOrPropertiesExcept("id", "created", "modified");
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getDisplayName()).isEqualTo(displayName);
		assertThat(entity.getShortCode()).isEqualTo(shortCode);
	}

	@Test
	void toNamespaceConfig() {
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var shortCode = "shortCode";
		final var displayName = "displayName";
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();

		final var entity = createEntity(municipalityId, namespace, shortCode, displayName, created, modified);

		final var config = mapper.toNamespaceConfig(entity);

		assertThat(config).hasNoNullFieldsOrProperties();
		assertThat(config.getNamespace()).isEqualTo(namespace);
		assertThat(config.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(config.getDisplayName()).isEqualTo(displayName);
		assertThat(config.getShortCode()).isEqualTo(shortCode);
		assertThat(config.getCreated()).isEqualTo(created);
		assertThat(config.getModified()).isEqualTo(modified);
	}

	@Test
	void toNamespaceConfigs() {
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var shortCode = "shortCode";
		final var displayName = "displayName";
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();

		final var entities = List.of(createEntity(municipalityId, namespace, shortCode, displayName, created, modified));

		final var configs = mapper.toNamespaceConfigs(entities);

		assertThat(configs).hasSize(1).satisfiesExactly(config -> {
			assertThat(config).hasNoNullFieldsOrProperties();
			assertThat(config.getNamespace()).isEqualTo(namespace);
			assertThat(config.getMunicipalityId()).isEqualTo(municipalityId);
			assertThat(config.getDisplayName()).isEqualTo(displayName);
			assertThat(config.getShortCode()).isEqualTo(shortCode);
			assertThat(config.getCreated()).isEqualTo(created);
			assertThat(config.getModified()).isEqualTo(modified);
		});
	}

	@Test
	void toNamespaceConfigsFromNull() {
		assertThat(mapper.toNamespaceConfigs(null)).isEmpty();
	}

	private static NamespaceConfigEntity createEntity(final String municipalityId, final String namespace, final String shortCode, final String displayName, final OffsetDateTime created, final OffsetDateTime modified) {
		return NamespaceConfigEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withDisplayName(displayName)
			.withShortCode(shortCode)
			.withCreated(created)
			.withModified(modified);
	}
}

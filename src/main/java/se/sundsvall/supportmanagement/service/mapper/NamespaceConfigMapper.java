package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import java.util.List;

import org.springframework.stereotype.Component;

import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;

@Component
public class NamespaceConfigMapper {

	public NamespaceConfigEntity toEntity(NamespaceConfig config, String namespace, String municipalityId) {
		return NamespaceConfigEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withDisplayName(config.getDisplayName())
			.withShortCode(config.getShortCode());
	}

	public List<NamespaceConfig> toNamespaceConfigs(List<NamespaceConfigEntity> entities) {
		return ofNullable(entities).orElse(emptyList())
			.stream()
			.map(this::toNamespaceConfig)
			.toList();
	}

	public NamespaceConfig toNamespaceConfig(NamespaceConfigEntity entity) {
		return NamespaceConfig.create()
			.withNamespace(entity.getNamespace())
			.withMunicipalityId(entity.getMunicipalityId())
			.withDisplayName(entity.getDisplayName())
			.withShortCode(entity.getShortCode())
			.withCreated(entity.getCreated())
			.withModified(entity.getModified());
	}
}

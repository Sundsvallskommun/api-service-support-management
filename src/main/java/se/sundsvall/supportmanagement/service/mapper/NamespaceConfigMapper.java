package se.sundsvall.supportmanagement.service.mapper;

import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;

@Component
public class NamespaceConfigMapper {

	public NamespaceConfigEntity toEntity(NamespaceConfig config, String namespace, String municipalityId) {
		return NamespaceConfigEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withShortCode(config.getShortCode());
	}

	public NamespaceConfig toNamespaceConfig(NamespaceConfigEntity entity) {
		return NamespaceConfig.create()
			.withShortCode(entity.getShortCode())
			.withCreated(entity.getCreated())
			.withModified(entity.getModified());
	}
}

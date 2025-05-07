package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;

@Component
public class NamespaceConfigMapper {

	private static final int DEFAULT_NOTIFICATION_TTL_IN_DAYS = 40;

	public NamespaceConfigEntity toEntity(NamespaceConfig config, String namespace, String municipalityId) {
		return NamespaceConfigEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withDisplayName(config.getDisplayName())
			.withShortCode(config.getShortCode())
			.withNotificationTTLInDays(Optional.ofNullable(config.getNotificationTTLInDays()).orElse(DEFAULT_NOTIFICATION_TTL_IN_DAYS));
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
			.withNotificationTTLInDays(entity.getNotificationTTLInDays())
			.withCreated(entity.getCreated())
			.withModified(entity.getModified());
	}
}

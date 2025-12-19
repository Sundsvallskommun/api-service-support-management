package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.BOOLEAN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.INTEGER;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.STRING;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_ACCESS_CONTROL;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_DISPLAY_NAME;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_NOTIFICATION_TTL_IN_DAYS;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_NOTIFY_REPORTER;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_SHORT_CODE;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.getRequiredValue;

import java.util.List;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.enums.ValueType;

@Component
public class NamespaceConfigMapper {

	private static final int DEFAULT_NOTIFICATION_TTL_IN_DAYS = 40;

	public NamespaceConfigEntity toEntity(final NamespaceConfig config, final String namespace, final String municipalityId) {
		return NamespaceConfigEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withValue(toNamespaceConfigPropertyEmbeddable(PROPERTY_DISPLAY_NAME, config.getDisplayName(), STRING))
			.withValue(toNamespaceConfigPropertyEmbeddable(PROPERTY_SHORT_CODE, config.getShortCode(), STRING))
			.withValue(toNamespaceConfigPropertyEmbeddable(PROPERTY_ACCESS_CONTROL, String.valueOf(config.isAccessControl()), BOOLEAN))
			.withValue(toNamespaceConfigPropertyEmbeddable(PROPERTY_NOTIFY_REPORTER, String.valueOf(config.isNotifyReporter()), BOOLEAN))
			.withValue(toNamespaceConfigPropertyEmbeddable(PROPERTY_NOTIFICATION_TTL_IN_DAYS, String.valueOf(ofNullable(config.getNotificationTTLInDays()).orElse(DEFAULT_NOTIFICATION_TTL_IN_DAYS)), INTEGER));
	}

	public List<NamespaceConfig> toNamespaceConfigs(final List<NamespaceConfigEntity> entities) {
		return ofNullable(entities).orElse(emptyList())
			.stream()
			.map(this::toNamespaceConfig)
			.toList();
	}

	private NamespaceConfigValueEmbeddable toNamespaceConfigPropertyEmbeddable(String key, String value, ValueType type) {
		return NamespaceConfigValueEmbeddable.create()
			.withKey(key)
			.withValue(value)
			.withType(type);
	}

	public NamespaceConfig toNamespaceConfig(final NamespaceConfigEntity entity) {
		return NamespaceConfig.create()
			.withNamespace(entity.getNamespace())
			.withMunicipalityId(entity.getMunicipalityId())
			.withCreated(entity.getCreated())
			.withModified(entity.getModified())
			.withDisplayName(getRequiredValue(entity, PROPERTY_DISPLAY_NAME))
			.withShortCode(getRequiredValue(entity, PROPERTY_SHORT_CODE))
			.withAccessControl(getRequiredValue(entity, PROPERTY_ACCESS_CONTROL))
			.withNotifyReporter(getRequiredValue(entity, PROPERTY_NOTIFY_REPORTER))
			.withNotificationTTLInDays(getRequiredValue(entity, PROPERTY_NOTIFICATION_TTL_IN_DAYS));
	}
}

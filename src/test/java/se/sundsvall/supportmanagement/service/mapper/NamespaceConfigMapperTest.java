package se.sundsvall.supportmanagement.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.BOOLEAN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.INTEGER;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.STRING;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_ACCESS_CONTROL;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_DISPLAY_NAME;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_NOTIFICATION_TTL_IN_DAYS;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_NOTIFY_REPORTER;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_SHORT_CODE;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;
import se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor;

class NamespaceConfigMapperTest {

	private final NamespaceConfigMapper mapper = new NamespaceConfigMapper();

	private static NamespaceConfigEntity createEntity(final String municipalityId, final String namespace, final String shortCode, final String displayName,
		final OffsetDateTime created, final OffsetDateTime modified, final boolean accessControl, final boolean notifyReporter) {
		return NamespaceConfigEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withValues(List.of(
				NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_ACCESS_CONTROL).withType(BOOLEAN).withValue(String.valueOf(accessControl)),
				NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_DISPLAY_NAME).withType(STRING).withValue(displayName),
				NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_NOTIFICATION_TTL_IN_DAYS).withType(INTEGER).withValue(String.valueOf(40)),
				NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_NOTIFY_REPORTER).withType(BOOLEAN).withValue(String.valueOf(notifyReporter)),
				NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_SHORT_CODE).withType(STRING).withValue(shortCode)))
			.withCreated(created)
			.withModified(modified);
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void toEntity(boolean toggleValue) {
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var shortCode = "shortCode";
		final var displayName = "displayName";
		final var notificationTTLInDays = 123;

		final var config = NamespaceConfig.create()
			.withDisplayName(displayName)
			.withNotificationTTLInDays(notificationTTLInDays)
			.withShortCode(shortCode)
			.withAccessControl(toggleValue)
			.withNotifyReporter(!toggleValue);

		final var entity = mapper.toEntity(config, namespace, municipalityId);

		assertThat(entity).hasNoNullFieldsOrPropertiesExcept("id", "created", "modified");
		assertThat(entity.getValues()).allSatisfy(value -> assertThat(value).hasNoNullFieldsOrProperties());

		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);

		assertThat((String) ConfigPropertyExtractor.getValue(entity, PROPERTY_DISPLAY_NAME)).isEqualTo(displayName);
		assertThat((String) ConfigPropertyExtractor.getValue(entity, PROPERTY_SHORT_CODE)).isEqualTo(shortCode);
		assertThat((Integer) ConfigPropertyExtractor.getValue(entity, PROPERTY_NOTIFICATION_TTL_IN_DAYS)).isEqualTo(notificationTTLInDays);
		assertThat((Boolean) ConfigPropertyExtractor.getValue(entity, PROPERTY_ACCESS_CONTROL)).isEqualTo(toggleValue);
		assertThat((Boolean) ConfigPropertyExtractor.getValue(entity, PROPERTY_NOTIFY_REPORTER)).isEqualTo(!toggleValue);
	}

	@Test
	void toEntityWithMissingNotificationTTL() {
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var shortCode = "shortCode";
		final var displayName = "displayName";
		final var accessControl = true;
		final var notifyReporter = true;
		final var config = NamespaceConfig.create()
			.withDisplayName(displayName)
			.withShortCode(shortCode)
			.withAccessControl(accessControl)
			.withNotifyReporter(notifyReporter);

		final var entity = mapper.toEntity(config, namespace, municipalityId);

		assertThat(entity).hasNoNullFieldsOrPropertiesExcept("id", "created", "modified");
		assertThat(entity.getValues()).allSatisfy(value -> assertThat(value).hasNoNullFieldsOrProperties());

		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);

		assertThat((String) ConfigPropertyExtractor.getValue(entity, PROPERTY_DISPLAY_NAME)).isEqualTo(displayName);
		assertThat((String) ConfigPropertyExtractor.getValue(entity, PROPERTY_SHORT_CODE)).isEqualTo(shortCode);
		assertThat((Integer) ConfigPropertyExtractor.getValue(entity, PROPERTY_NOTIFICATION_TTL_IN_DAYS)).isEqualTo(40);
		assertThat((Boolean) ConfigPropertyExtractor.getValue(entity, PROPERTY_ACCESS_CONTROL)).isEqualTo(accessControl);
		assertThat((Boolean) ConfigPropertyExtractor.getValue(entity, PROPERTY_NOTIFY_REPORTER)).isEqualTo(notifyReporter);
	}

	@Test
	void toNamespaceConfig() {
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var shortCode = "shortCode";
		final var displayName = "displayName";
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var accessControl = true;
		final var notifyReporter = true;

		final var entity = createEntity(municipalityId, namespace, shortCode, displayName, created, modified, accessControl, notifyReporter);

		final var config = mapper.toNamespaceConfig(entity);

		assertThat(config).hasNoNullFieldsOrPropertiesExcept("notificationTTLInDays");
		assertThat(config.getNamespace()).isEqualTo(namespace);
		assertThat(config.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(config.getDisplayName()).isEqualTo(displayName);
		assertThat(config.getShortCode()).isEqualTo(shortCode);
		assertThat(config.getCreated()).isEqualTo(created);
		assertThat(config.getModified()).isEqualTo(modified);
		assertThat(config.isAccessControl()).isEqualTo(accessControl);
		assertThat(config.isNotifyReporter()).isEqualTo(notifyReporter);
	}

	@Test
	void toNamespaceConfigs() {
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var shortCode = "shortCode";
		final var displayName = "displayName";
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var accessControl = true;
		final var notifyReporter = true;

		final var entities = List.of(createEntity(municipalityId, namespace, shortCode, displayName, created, modified, accessControl, notifyReporter));

		final var configs = mapper.toNamespaceConfigs(entities);

		assertThat(configs).hasSize(1).satisfiesExactly(config -> {
			assertThat(config).hasNoNullFieldsOrProperties();
			assertThat(config.getNamespace()).isEqualTo(namespace);
			assertThat(config.getMunicipalityId()).isEqualTo(municipalityId);
			assertThat(config.getDisplayName()).isEqualTo(displayName);
			assertThat(config.getShortCode()).isEqualTo(shortCode);
			assertThat(config.getCreated()).isEqualTo(created);
			assertThat(config.getModified()).isEqualTo(modified);
			assertThat(config.isAccessControl()).isEqualTo(accessControl);
			assertThat(config.isNotifyReporter()).isEqualTo(notifyReporter);
		});
	}

	@Test
	void toNamespaceConfigsFromNull() {
		assertThat(mapper.toNamespaceConfigs(null)).isEmpty();
	}
}

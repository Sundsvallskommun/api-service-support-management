package se.sundsvall.supportmanagement.service.mapper;

import generated.se.sundsvall.accessmapper.Access;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.api.model.config.FieldAccess;
import se.sundsvall.supportmanagement.api.model.config.LimitedReadAccess;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.api.model.config.ReporterAccess;
import se.sundsvall.supportmanagement.api.model.config.ResourceAccess;
import se.sundsvall.supportmanagement.api.model.config.RoleFieldRestriction;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigAccessGrantEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.enums.AccessGrantScope;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.db.model.enums.ValueType;
import se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static se.sundsvall.supportmanagement.integration.db.model.enums.AccessGrantScope.LIMITED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.AccessGrantScope.REPORTER;
import static se.sundsvall.supportmanagement.integration.db.model.enums.RoleAccessType.FIELD;
import static se.sundsvall.supportmanagement.integration.db.model.enums.RoleAccessType.RESOURCE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.BOOLEAN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.INTEGER;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.STRING;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_ACCESS_CONTROL;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_DISPLAY_NAME;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_NOTIFICATION_TTL_IN_DAYS;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_NOTIFY_REPORTER;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_RESOURCE_ACCESS_CONTROL;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_ROLE_BASED_MAPPING;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_SHORT_CODE;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.getValue;

@Component
public class NamespaceConfigMapper {

	private static final Logger LOG = LoggerFactory.getLogger(NamespaceConfigMapper.class);

	private static final int DEFAULT_NOTIFICATION_TTL_IN_DAYS = 40;

	/**
	 * Separates the field name from the key it is limited to, e.g. "PARAMETERS:contactChannel". Only the first occurrence
	 * separates, so keys may themselves contain the separator.
	 */
	private static final String KEY_SEPARATOR = ":";

	public NamespaceConfigEntity toEntity(final NamespaceConfig config, final String namespace, final String municipalityId) {
		return NamespaceConfigEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withValue(toNamespaceConfigPropertyEmbeddable(PROPERTY_DISPLAY_NAME, config.getDisplayName(), STRING))
			.withValue(toNamespaceConfigPropertyEmbeddable(PROPERTY_SHORT_CODE, config.getShortCode(), STRING))
			.withValue(toNamespaceConfigPropertyEmbeddable(PROPERTY_ACCESS_CONTROL, String.valueOf(config.isAccessControl()), BOOLEAN))
			.withValue(toNamespaceConfigPropertyEmbeddable(PROPERTY_NOTIFY_REPORTER, String.valueOf(config.isNotifyReporter()), BOOLEAN))
			.withValue(toNamespaceConfigPropertyEmbeddable(PROPERTY_ROLE_BASED_MAPPING, String.valueOf(config.isRoleBasedMapping()), BOOLEAN))
			.withValue(toNamespaceConfigPropertyEmbeddable(PROPERTY_RESOURCE_ACCESS_CONTROL, String.valueOf(config.isResourceAccessControl()), BOOLEAN))
			.withValue(toNamespaceConfigPropertyEmbeddable(PROPERTY_NOTIFICATION_TTL_IN_DAYS, String.valueOf(ofNullable(config.getNotificationTTLInDays()).orElse(DEFAULT_NOTIFICATION_TTL_IN_DAYS)), INTEGER))
			.withAccessGrants(toAccessGrants(config));
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
			.withDisplayName(getValue(entity, PROPERTY_DISPLAY_NAME))
			.withShortCode(getValue(entity, PROPERTY_SHORT_CODE))
			.withAccessControl(getValue(entity, PROPERTY_ACCESS_CONTROL))
			.withNotifyReporter(getValue(entity, PROPERTY_NOTIFY_REPORTER))
			.withRoleBasedMapping(readOptionalToggle(entity, PROPERTY_ROLE_BASED_MAPPING))
			.withResourceAccessControl(readOptionalToggle(entity, PROPERTY_RESOURCE_ACCESS_CONTROL))
			.withNotificationTTLInDays(getValue(entity, PROPERTY_NOTIFICATION_TTL_IN_DAYS))
			.withLimitedReadAccess(toLimitedReadAccess(entity))
			.withReporterAccess(toReporterAccess(entity))
			.withRoleFieldRestrictions(toRoleAccesses(entity));
	}

	/**
	 * Toggles added after a configuration was created read as disabled rather than failing the whole request.
	 */
	private boolean readOptionalToggle(final NamespaceConfigEntity entity, final String key) {
		return ofNullable(ConfigPropertyExtractor.<Boolean>getNullableValue(entity, key)).orElse(false);
	}

	/**
	 * Flattens the configured access into one row per grant, keyed by scope. A field without keys becomes a single row, a
	 * field with keys becomes one row per key.
	 */
	private List<NamespaceConfigAccessGrantEmbeddable> toAccessGrants(final NamespaceConfig config) {
		final var grants = new ArrayList<NamespaceConfigAccessGrantEmbeddable>();

		ofNullable(config.getLimitedReadAccess()).ifPresent(limitedReadAccess -> {
			// Limited read is read only, so a limited resource grant carries no access level.
			ofNullable(limitedReadAccess.getResources()).orElse(emptyList()).forEach(resource -> grants.add(NamespaceConfigAccessGrantEmbeddable.create()
				.withScope(LIMITED.name())
				.withType(RESOURCE)
				.withValue(resource.name())));

			addFieldGrants(grants, LIMITED.name(), limitedReadAccess.getFields());
		});

		addReporterAccess(grants, config.getReporterAccess());

		ofNullable(config.getRoleFieldRestrictions()).orElse(emptyList())
			.forEach(roleFieldRestrictions -> addFieldGrants(grants, roleFieldRestrictions.getRole(), roleFieldRestrictions.getFields()));

		return grants;
	}

	private void addReporterAccess(final List<NamespaceConfigAccessGrantEmbeddable> grants, final ReporterAccess access) {
		ofNullable(access).ifPresent(reporterAccess -> {
			ofNullable(reporterAccess.getResources()).orElse(emptyList()).forEach(resource -> grants.add(NamespaceConfigAccessGrantEmbeddable.create()
				.withScope(REPORTER.name())
				.withType(RESOURCE)
				.withValue(resource.getResource().name())
				.withAccessLevel(resource.getLevel().name())));

			addFieldGrants(grants, REPORTER.name(), reporterAccess.getFields());
		});
	}

	private void addFieldGrants(final List<NamespaceConfigAccessGrantEmbeddable> grants, final String scope, final List<FieldAccess> fields) {
		ofNullable(fields).orElse(emptyList()).forEach(field -> {
			if (isEmpty(field.getKeys())) {
				grants.add(toFieldGrant(scope, field.getField().name()));
			} else {
				field.getKeys().forEach(key -> grants.add(toFieldGrant(scope, field.getField().name() + KEY_SEPARATOR + key)));
			}
		});
	}

	private NamespaceConfigAccessGrantEmbeddable toFieldGrant(final String scope, final String value) {
		return NamespaceConfigAccessGrantEmbeddable.create()
			.withScope(scope)
			.withType(FIELD)
			.withValue(value);
	}

	/**
	 * Rebuilds the fields exposed to each role supplied by the access mapper. Reserved scopes are left out, they are
	 * returned through their own parts of the configuration.
	 */
	private List<RoleFieldRestriction> toRoleAccesses(final NamespaceConfigEntity entity) {
		final var roles = ofNullable(entity.getAccessGrants()).orElse(emptyList()).stream()
			.map(NamespaceConfigAccessGrantEmbeddable::getScope)
			.filter(scope -> !EnumUtils.isValidEnumIgnoreCase(AccessGrantScope.class, scope))
			.distinct()
			.sorted()
			.map(scope -> RoleFieldRestriction.create()
				.withRole(scope)
				.withFields(toFieldAccesses(grantsOfScope(entity, scope))))
			.toList();

		return roles.isEmpty() ? null : roles;
	}

	private LimitedReadAccess toLimitedReadAccess(final NamespaceConfigEntity entity) {
		final var grants = grantsOfScope(entity, LIMITED.name());

		return grants.isEmpty() ? null
			: LimitedReadAccess.create()
				.withResources(toResources(grants))
				.withFields(toFieldAccesses(grants));
	}

	private ReporterAccess toReporterAccess(final NamespaceConfigEntity entity) {
		final var grants = grantsOfScope(entity, REPORTER.name());

		return grants.isEmpty() ? null
			: ReporterAccess.create()
				.withResources(toResourceAccesses(grants))
				.withFields(toFieldAccesses(grants));
	}

	/**
	 * Values that no longer resolve to a known resource are skipped, so a stale row cannot make the whole configuration
	 * unreadable.
	 */
	private List<ProtectedResource> toResources(final List<NamespaceConfigAccessGrantEmbeddable> grants) {
		final var resources = grants.stream()
			.filter(grant -> RESOURCE == grant.getType())
			.map(grant -> {
				final var resource = EnumUtils.getEnum(ProtectedResource.class, grant.getValue());
				if (resource == null) {
					LOG.warn("Skipping unknown resource grant '{}' for scope '{}'", grant.getValue(), grant.getScope());
				}
				return resource;
			})
			.filter(Objects::nonNull)
			.toList();

		return resources.isEmpty() ? null : resources;
	}

	private List<NamespaceConfigAccessGrantEmbeddable> grantsOfScope(final NamespaceConfigEntity entity, final String scope) {
		return ofNullable(entity.getAccessGrants()).orElse(emptyList()).stream()
			.filter(grant -> scope.equalsIgnoreCase(grant.getScope()))
			.sorted(Comparator.comparing(NamespaceConfigAccessGrantEmbeddable::getValue))
			.toList();
	}

	/**
	 * Values that no longer resolve to a known resource or level are skipped, so a stale row cannot make the whole
	 * configuration unreadable.
	 */
	private List<ResourceAccess> toResourceAccesses(final List<NamespaceConfigAccessGrantEmbeddable> grants) {
		final var resources = grants.stream()
			.filter(grant -> RESOURCE == grant.getType())
			.map(grant -> {
				final var resource = EnumUtils.getEnum(ProtectedResource.class, grant.getValue());
				final var level = EnumUtils.getEnum(Access.AccessLevelEnum.class, grant.getAccessLevel());

				if (resource == null || level == null) {
					LOG.warn("Skipping unknown resource grant '{}' with level '{}' for scope '{}'", grant.getValue(), grant.getAccessLevel(), grant.getScope());
					return null;
				}
				return ResourceAccess.create().withResource(resource).withLevel(level);
			})
			.filter(Objects::nonNull)
			.toList();

		return resources.isEmpty() ? null : resources;
	}

	/**
	 * Values that no longer resolve to a known field are skipped, for the same reason.
	 */
	private List<FieldAccess> toFieldAccesses(final List<NamespaceConfigAccessGrantEmbeddable> grants) {
		final Map<ErrandField, List<String>> keysByField = new LinkedHashMap<>();

		grants.stream()
			.filter(grant -> FIELD == grant.getType())
			.forEach(grant -> {
				final var separatorIndex = grant.getValue().indexOf(KEY_SEPARATOR);
				final var fieldName = separatorIndex < 0 ? grant.getValue() : grant.getValue().substring(0, separatorIndex);
				final var field = EnumUtils.getEnum(ErrandField.class, fieldName);

				if (field == null) {
					LOG.warn("Skipping unknown field grant '{}' for scope '{}'", grant.getValue(), grant.getScope());
					return;
				}

				final var keys = keysByField.computeIfAbsent(field, _ -> new ArrayList<>());
				if (separatorIndex >= 0) {
					keys.add(grant.getValue().substring(separatorIndex + 1));
				}
			});

		if (keysByField.isEmpty()) {
			return null;
		}

		return keysByField.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.map(entry -> FieldAccess.create()
				.withField(entry.getKey())
				.withKeys(entry.getValue().isEmpty() ? null : entry.getValue()))
			.toList();
	}
}

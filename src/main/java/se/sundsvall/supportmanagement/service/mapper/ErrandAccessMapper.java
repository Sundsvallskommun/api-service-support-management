package se.sundsvall.supportmanagement.service.mapper;

import generated.se.sundsvall.accessmapper.Access;
import java.util.List;
import java.util.Map;
import se.sundsvall.supportmanagement.api.model.access.ErrandAccess;
import se.sundsvall.supportmanagement.api.model.access.ErrandFieldAccess;
import se.sundsvall.supportmanagement.api.model.access.ErrandFieldKeyAccess;
import se.sundsvall.supportmanagement.api.model.access.ErrandResourceAccess;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.service.AccessControlService.ErrandAccessResolution;
import se.sundsvall.supportmanagement.service.AccessControlService.FieldGrant;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

public final class ErrandAccessMapper {

	private ErrandAccessMapper() {}

	public static ErrandAccess toErrandAccess(final ErrandAccessResolution resolution) {
		if (isNull(resolution)) {
			return null;
		}

		return ErrandAccess.create()
			.withLevel(toAccessLevel(resolution.errandLevel()))
			.withFields(resolution.fields().entrySet().stream()
				.map(entry -> toErrandFieldAccess(entry.getKey(), entry.getValue()))
				.toList())
			.withResources(resolution.resources().entrySet().stream()
				.map(entry -> ErrandResourceAccess.create()
					.withResource(entry.getKey().getPath())
					.withLevel(toAccessLevel(entry.getValue())))
				.toList());
	}

	/**
	 * Reports the field by the property it names on the errand, so that a client can match it against the payload it
	 * renders.
	 */
	private static ErrandFieldAccess toErrandFieldAccess(final ErrandField field, final FieldGrant grant) {
		return ErrandFieldAccess.create()
			.withField(field.getPropertyName())
			.withLevel(toAccessLevel(grant.level()))
			.withAllKeys(grant.allKeys())
			.withKeys(toKeys(grant.keys()));
	}

	private static List<ErrandFieldKeyAccess> toKeys(final Map<String, Access.AccessLevelEnum> keys) {
		if (isNull(keys)) {
			return null;
		}

		return keys.entrySet().stream()
			.map(entry -> ErrandFieldKeyAccess.create()
				.withKey(entry.getKey())
				.withLevel(toAccessLevel(entry.getValue())))
			.toList();
	}

	/**
	 * Translates a level of the access mapper contract into the level of the same name this API publishes, and null
	 * into null.
	 */
	private static AccessLevel toAccessLevel(final Access.AccessLevelEnum level) {
		return ofNullable(level)
			.map(value -> AccessLevel.valueOf(value.name()))
			.orElse(null);
	}
}

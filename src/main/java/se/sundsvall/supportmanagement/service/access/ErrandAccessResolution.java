package se.sundsvall.supportmanagement.service.access;

import generated.se.sundsvall.accessmapper.Access;
import java.util.Map;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

/**
 * What a user may do with one errand, resolved in one pass.
 *
 * @param errandLevel the level they hold the errand itself at, never null
 * @param resources   the level they hold each errand scoped resource they reach at, the errand itself excluded
 * @param fields      what they may do with each field they reach
 */
public record ErrandAccessResolution(
	Access.AccessLevelEnum errandLevel,
	Map<ProtectedResource, Access.AccessLevelEnum> resources,
	Map<ErrandField, FieldGrant> fields) {}

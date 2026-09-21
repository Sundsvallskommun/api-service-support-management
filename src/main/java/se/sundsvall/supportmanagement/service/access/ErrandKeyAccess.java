package se.sundsvall.supportmanagement.service.access;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;

/**
 * What a caller may do with the keyed fields of one errand they are patching.
 *
 * @param writableKey the keys they may change, per field, for the merge to leave the rest as it stands
 * @param readable    the fields they may see, for the response to be mapped as a plain read would be
 */
public record ErrandKeyAccess(Function<ErrandField, Predicate<String>> writableKey, Function<ErrandEntity, Map<ErrandField, Set<String>>> readable) {}

package se.sundsvall.supportmanagement.service.access;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;

import static java.util.Objects.isNull;

/**
 * What the user may read of one errand and what of it they may write, resolved together.
 * <p>
 * A null map means nothing restricts the user and the errand is reached in full, which is what an unrestricted role
 * yields. An empty map, in contrast, is a restriction resolving to no fields whatsoever.
 *
 * @param readable the fields, and the keys of them, the user may see
 * @param writable the fields, and the keys of them, the user may change
 */
public record FieldAccessResolution(Map<ErrandField, Set<String>> readable, Map<ErrandField, Set<String>> writable) {

	public static FieldAccessResolution unrestricted() {
		return new FieldAccessResolution(null, null);
	}

	/**
	 * The keys of sent in field the user may see.
	 */
	public Predicate<String> readableKey(ErrandField field) {
		return toKeyPredicate(readable, field);
	}

	/**
	 * The keys of sent in field the user may change. Never wider than {@link #readableKey}.
	 */
	public Predicate<String> writableKey(ErrandField field) {
		return toKeyPredicate(writable, field);
	}

	/**
	 * Reads one field out of a resolved map. A null map is a user nothing restricts, so every key of every field is
	 * theirs; a field the map does not carry is one they do not reach at all; and a field carrying no keys is the
	 * whole collection.
	 */
	private static Predicate<String> toKeyPredicate(Map<ErrandField, Set<String>> fields, ErrandField field) {
		if (isNull(fields)) {
			return _ -> true;
		}

		final var keys = fields.get(field);
		if (isNull(keys)) {
			return _ -> false;
		}

		return keys.isEmpty() ? _ -> true : keys::contains;
	}
}

package se.sundsvall.supportmanagement.integration.db.model.enums;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * How a field of the errand is held by the search index: which index fields carry it, which of them an ordering by a
 * property of the field sorts on, which of them a count groups by, and whether the keys of a keyed field are paths of
 * their own in the index.
 *
 * @param fields       the index fields, by their names or, for a name ending in a dot, by the start of their names
 * @param sorts        the index field an ordering sorts on, by the property the ordering names. The empty key stands
 *                     for the property of the field itself
 * @param groups       the index field a count groups by, keyed the same way. Only the single valued fields the index
 *                     can aggregate: a multi valued one would put an errand in several buckets and make the buckets
 *                     add up to more than the count beside them
 * @param keysArePaths true when the keys of the keyed field are the next segment of the index field names below it,
 *                     so that a key can be told apart from another in a search; false when the values of every key
 *                     share the same fields
 */
public record IndexBinding(List<String> fields, Map<String, String> sorts, Map<String, String> groups, boolean keysArePaths) {

	static final String OWN_PROPERTY = "";

	public static IndexBinding of(final String... fields) {
		return new IndexBinding(List.of(fields), Map.of(), Map.of(), false);
	}

	/** A field the index does not hold. */
	public static IndexBinding none() {
		return new IndexBinding(List.of(), Map.of(), Map.of(), false);
	}

	/** An ordering by the field's own property sorts on sent in index field. */
	public IndexBinding sortedBy(final String field) {
		return sortedBy(OWN_PROPERTY, field);
	}

	/** An ordering by sent in property of the field sorts on sent in index field. */
	public IndexBinding sortedBy(final String property, final String field) {
		final var withSort = new LinkedHashMap<>(sorts);
		withSort.put(property, field);
		return new IndexBinding(fields, Map.copyOf(withSort), groups, keysArePaths);
	}

	/** A count grouping by the field's own property groups by sent in index field. */
	public IndexBinding groupedBy(final String field) {
		return groupedBy(OWN_PROPERTY, field);
	}

	/** A count grouping by sent in property of the field groups by sent in index field. */
	public IndexBinding groupedBy(final String property, final String field) {
		final var withGroup = new LinkedHashMap<>(groups);
		withGroup.put(property, field);
		return new IndexBinding(fields, sorts, Map.copyOf(withGroup), keysArePaths);
	}

	public IndexBinding withKeysAsPaths() {
		return new IndexBinding(fields, sorts, groups, true);
	}
}

package se.sundsvall.supportmanagement.service.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

import static java.util.Objects.isNull;

/**
 * Which index fields one route of a grant keeps closed, and why.
 * <p>
 * Three things close a field. A resource the labels of the user do not reach. A field their roles keep from them. And
 * a key of a keyed field their roles keep from them: for JSON parameters the keys are paths of their own and the other
 * keys stay open, while parameter values and tag values sit in fields shared by every key, which therefore close as a
 * whole.
 *
 * @param rules what is closed, empty when everything is open
 */
record FieldClosure(List<Closed> rules) {

	static final FieldClosure OPEN = new FieldClosure(List.of());

	/**
	 * One closed field or start of field names, and why.
	 *
	 * @param name        the field, or the start of the names of the fields when ending in a dot
	 * @param openKeys    for a keyed field, the keys that stay open under the name, null when the whole name is closed
	 * @param description what is closed, for the answer to the client
	 */
	record Closed(String name, Set<String> openKeys, String description) {

		Optional<String> refusal(final String field) {
			if (!matches(field)) {
				return Optional.empty();
			}
			if (isNull(openKeys)) {
				return Optional.of(description);
			}
			final var key = field.substring(name.length()).split("\\.", 2)[0];
			return openKeys.contains(key) ? Optional.empty() : Optional.of("Key '%s' of %s".formatted(key, description));
		}

		private boolean matches(final String field) {
			return name.endsWith(".") ? field.startsWith(name) : field.equals(name) || field.startsWith(name + ".");
		}
	}

	/**
	 * The closure of one route: the resources it does not reach, and the fields it may not read.
	 *
	 * @param resources the resources the route reaches on every errand it reaches
	 * @param readable  what the route may read, null when nothing restricts it
	 */
	static FieldClosure of(final Set<ProtectedResource> resources, final Map<ErrandField, Set<String>> readable) {
		final var rules = new ArrayList<Closed>();
		closedResources(resources, rules);
		closedFields(readable, rules);
		return new FieldClosure(List.copyOf(rules));
	}

	/** The fields of the resources the route does not reach, whole. */
	private static void closedResources(final Set<ProtectedResource> resources, final List<Closed> rules) {
		for (final var resource : ProtectedResource.values()) {
			if (!resource.getSearchFields().isEmpty() && !resources.contains(resource)) {
				resource.getSearchFields().forEach(name -> rules.add(new Closed(name, null, "Resource '%s'".formatted(resource.getPath()))));
			}
		}
	}

	/**
	 * The fields the roles of the user keep from them. A null map restricts nothing; a field the map does not carry is
	 * closed; a keyed field carrying keys keeps those keys open where the index can tell them apart.
	 */
	private static void closedFields(final Map<ErrandField, Set<String>> readable, final List<Closed> rules) {
		if (isNull(readable)) {
			return;
		}

		for (final var field : ErrandField.values()) {
			final var keys = readable.get(field);
			final var description = "Field '%s'".formatted(field.getPropertyName());

			if (isNull(keys)) {
				field.getSearchFields().forEach(name -> rules.add(new Closed(name, null, description)));
			} else if (!keys.isEmpty()) {
				// Keys that are paths keep the granted ones open under the object; anything else the keys share closes
				field.getSearchFields().forEach(name -> rules.add(field.getIndex().keysArePaths() && name.endsWith(".")
					? new Closed(name, keys, description)
					: new Closed(name, null, description + " beyond its keys")));
			}
		}
	}

	boolean isOpen() {
		return rules.isEmpty();
	}

	/** Why sent in field is refused, empty when it is open. */
	Optional<String> refusal(final String field) {
		return rules.stream()
			.map(rule -> rule.refusal(field))
			.flatMap(Optional::stream)
			.findFirst();
	}

	boolean allows(final String field) {
		return refusal(field).isEmpty();
	}

	/** Those of sent in fields that are open. */
	List<String> openFields(final List<String> fields) {
		return fields.stream().filter(this::allows).toList();
	}
}

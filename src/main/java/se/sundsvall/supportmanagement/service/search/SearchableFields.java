package se.sundsvall.supportmanagement.service.search;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

import static java.util.Objects.isNull;

/**
 * Which index fields one route of a grant may search.
 * <p>
 * This used to say what a route kept closed, and everything it did not name stayed open. Every time the reading of a
 * query turned out to differ from the parser's - a name opened by a minus, a unicode escape, the value of
 * {@code _exists_}, the name of an object rather than a field under it - the difference was a field searched without
 * being granted, because being unrecognised meant being permitted. So it says what is open instead: a name is
 * searchable only where it is bound to a field or a resource the route reaches, and a name nobody bound is refused
 * whether or not anyone thought of it.
 * <p>
 * A name is open when both hold, and it is bound to at least one of the two:
 * <ul>
 * <li>every {@link ErrandField} it belongs to is readable on the route, or nothing restricts the route at all</li>
 * <li>every {@link ProtectedResource} it belongs to is reached by the route</li>
 * </ul>
 * A keyed field granted single keys opens those keys where the index tells them apart, and opens nothing where the keys
 * share a field.
 *
 * @param readable  what the route may read, null where nothing restricts it
 * @param resources the resources the route reaches on every errand it reaches
 */
record SearchableFields(Map<ErrandField, Set<String>> readable, Set<ProtectedResource> resources) {

	/** What the answer to the client calls a name it cannot place among the fields of an errand. */
	static final String UNKNOWN = "'%s'";
	static final String FIELD = "Field '%s'";
	static final String RESOURCE = "Resource '%s'";
	static final String KEY_OF_FIELD = "Key '%s' of Field '%s'";
	static final String FIELD_BEYOND_KEYS = "Field '%s' beyond its keys";

	/** The keyword twin of a text field, which follows the field it doubles. */
	private static final String RAW = ".raw";

	static SearchableFields of(final Set<ProtectedResource> resources, final Map<ErrandField, Set<String>> readable) {
		return new SearchableFields(readable, resources);
	}

	/**
	 * Whether the route is held to nothing: every field readable and every resource reached. A wildcard in a name stands
	 * for fields nobody enumerated, so it is only ever allowed here.
	 */
	boolean unrestricted() {
		return isNull(readable) && guardedResources().allMatch(resources::contains);
	}

	/** Why sent in name may not be searched, empty where it may. */
	Optional<String> refusal(final String name) {
		final var bare = name.endsWith(RAW) ? name.substring(0, name.length() - RAW.length()) : name;

		final var closedResource = guardedResources()
			.filter(resource -> !resources.contains(resource))
			.filter(resource -> resource.getSearchFields().stream().anyMatch(binding -> covers(binding, bare)))
			.findFirst();
		if (closedResource.isPresent()) {
			return Optional.of(RESOURCE.formatted(closedResource.get().getPath()));
		}

		final var boundFields = Stream.of(ErrandField.values())
			.filter(field -> field.getSearchFields().stream().anyMatch(binding -> covers(binding, bare)))
			.toList();
		final var boundResource = guardedResources()
			.anyMatch(resource -> resource.getSearchFields().stream().anyMatch(binding -> covers(binding, bare)));

		if (boundFields.isEmpty()) {
			// A resource of its own carries fields no ErrandField names, such as the subject of a communication
			return boundResource ? Optional.empty() : Optional.of(UNKNOWN.formatted(name));
		}

		return boundFields.stream()
			.map(field -> fieldRefusal(field, bare))
			.flatMap(Optional::stream)
			.findFirst();
	}

	boolean allows(final String name) {
		return refusal(name).isEmpty();
	}

	/**
	 * Why the route may not order by sent in field, empty when it may.
	 * <p>
	 * Asked of the field rather than of the index field it sorts on: a sort field is a twin of the field it orders, named
	 * for the index alone, and holding a name like {@code title_sort} against the fields a route may search would refuse
	 * every sort and name something no client wrote.
	 */
	Optional<String> sortRefusal(final ErrandField field) {
		final var closedResource = guardedResources()
			.filter(resource -> !resources.contains(resource))
			.filter(resource -> resource.getSearchFields().stream()
				.anyMatch(binding -> field.getSearchFields().stream().anyMatch(name -> covers(binding, name))))
			.findFirst();
		if (closedResource.isPresent()) {
			return Optional.of(RESOURCE.formatted(closedResource.get().getPath()));
		}

		return isNull(readable) || readable.containsKey(field)
			? Optional.empty()
			: Optional.of(FIELD.formatted(field.getPropertyName()));
	}

	/** Those of sent in fields the route may search, which is what a word without a field is looked for in. */
	List<String> openFields(final List<String> fields) {
		return fields.stream().filter(this::allows).toList();
	}

	/**
	 * Whether the route may search sent in field, and where a keyed field is granted single keys, whether the name stays
	 * within them.
	 */
	private Optional<String> fieldRefusal(final ErrandField field, final String name) {
		if (isNull(readable)) {
			return Optional.empty();
		}

		final var keys = readable.get(field);
		if (isNull(keys)) {
			return Optional.of(FIELD.formatted(field.getPropertyName()));
		}
		if (keys.isEmpty()) {
			// The whole field, keys and all
			return Optional.empty();
		}

		if (!field.getIndex().keysArePaths()) {
			// Every key of this field writes into the same index field, so single keys open none of it
			return Optional.of(FIELD_BEYOND_KEYS.formatted(field.getPropertyName()));
		}

		return keyRefusal(field, name, keys);
	}

	/**
	 * A keyed field whose keys are paths of their own: what follows the object begins with the key, and a key holds no
	 * dots (see {@code JSON_PARAMETER_KEY_REGEXP}), so the key is the first segment of that path, whole.
	 */
	private Optional<String> keyRefusal(final ErrandField field, final String name, final Set<String> keys) {
		final var prefix = field.getSearchFields().stream()
			.filter(binding -> binding.endsWith(".") && covers(binding, name))
			.findFirst();

		if (prefix.isEmpty()) {
			// The catch-all text of the field, which holds the values of every key together
			return Optional.of(FIELD_BEYOND_KEYS.formatted(field.getPropertyName()));
		}

		final var key = name.substring(prefix.get().length()).split("\\.", 2)[0];
		return keys.contains(key) ? Optional.empty() : Optional.of(KEY_OF_FIELD.formatted(key, field.getPropertyName()));
	}

	/**
	 * Whether a binding covers sent in name: a name ending in a dot stands for the object and everything under it, and
	 * anything else for itself.
	 */
	private static boolean covers(final String binding, final String name) {
		if (!binding.endsWith(".")) {
			return name.equals(binding);
		}
		final var object = binding.substring(0, binding.length() - 1);
		return name.equals(object) || name.startsWith(binding);
	}

	/** The resources that carry fields of the index, which are the ones a search can be held to. */
	private static Stream<ProtectedResource> guardedResources() {
		return Stream.of(ProtectedResource.values()).filter(resource -> !resource.getSearchFields().isEmpty());
	}
}

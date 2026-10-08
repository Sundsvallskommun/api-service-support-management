package se.sundsvall.supportmanagement.service.mapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.model.ArtefactParameter;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public final class ErrandParameterMapper {

	/** Keys regardless of case, and keys that differ only in case in their natural order. */
	static final Comparator<Parameter> KEY_ORDER = comparing(Parameter::getKey, CASE_INSENSITIVE_ORDER).thenComparing(Parameter::getKey);

	private ErrandParameterMapper() {
		// Intentionally empty
	}

	public static List<ParameterEntity> toErrandParameterEntityList(final List<Parameter> parameters, ErrandEntity entity) {
		return new ArrayList<>(toUniqueKeyList(parameters).stream()
			.map(parameter -> toErrandParameterEntity(parameter).withErrandEntity(entity))
			.toList());
	}

	public static ParameterEntity toErrandParameterEntity(final Parameter parameter) {
		return ParameterEntity.create()
			.withDisplayName(parameter.getDisplayName())
			.withParameterGroup(parameter.getGroup())
			.withKey(parameter.getKey())
			.withValues(parameter.getValues());
	}

	public static Parameter toParameter(final ParameterEntity parameter) {
		return Parameter.create()
			.withDisplayName(parameter.getDisplayName())
			.withGroup(parameter.getParameterGroup())
			.withKey(parameter.getKey())
			.withValues(parameter.getValues())
			.withVersion(parameter.getVersion());
	}

	public static void mergeParameters(final ErrandEntity entity, final List<Parameter> parameters) {
		mergeParameters(entity, parameters, _ -> true);
	}

	/**
	 * Replaces the parameters of the errand with sent in ones, leaving keys the caller may not change untouched.
	 * <p>
	 * A changeable key absent from the request is deleted. A key the caller may not change is left as it stands, whether or
	 * not the request carries it.
	 *
	 * @param entity      errand to merge into
	 * @param parameters  parameters replacing the changeable ones
	 * @param writableKey predicate accepting the keys the caller may change
	 */
	public static void mergeParameters(final ErrandEntity entity, final List<Parameter> parameters, final Predicate<String> writableKey) {
		if (entity.getParameters() == null) {
			entity.setParameters(new ArrayList<>());
		}
		final var existing = entity.getParameters();

		// A parameter the caller may not write is left exactly as it stands, whether or not the request carries it. It
		// carrying an unchanged one is how a caller patches back what they were served, and applying it again would bump
		// the version of data they may not change.
		final var uniqueIncoming = toUniqueKeyList(parameters).stream()
			.filter(parameter -> writableKey.test(parameter.getKey()))
			.toList();
		final var incomingByKey = uniqueIncoming.stream().collect(toMap(Parameter::getKey, identity()));
		final var existingByKey = existing.stream().collect(toMap(ParameterEntity::getKey, identity()));

		existing.removeIf(e -> writableKey.test(e.getKey()) && !incomingByKey.containsKey(e.getKey()));
		existing.stream()
			.filter(e -> incomingByKey.containsKey(e.getKey()))
			.forEach(e -> {
				final var incoming = incomingByKey.get(e.getKey());
				e.setDisplayName(incoming.getDisplayName());
				e.setParameterGroup(incoming.getGroup());
				e.setValues(incoming.getValues());
			});
		uniqueIncoming.stream()
			.filter(p -> !existingByKey.containsKey(p.getKey()))
			.map(p -> toErrandParameterEntity(p).withErrandEntity(entity))
			.forEach(existing::add);
	}

	/**
	 * The keys of sent in parameters that would come out of a merge different from how they stand on the errand, which is
	 * everything the merge writes: the values, the display name and the group. A key the errand does not carry at all is
	 * a change, since the merge would add it.
	 *
	 * @param  entity     errand the parameters would be merged into
	 * @param  parameters parameters of the request
	 * @return            keys the request would change, none when it carries no parameters at all
	 */
	public static List<String> changedKeys(final ErrandEntity entity, final List<Parameter> parameters) {
		if (isNull(parameters)) {
			return emptyList();
		}

		final var existingByKey = Optional.ofNullable(entity.getParameters()).orElse(emptyList()).stream()
			.collect(toMap(ParameterEntity::getKey, identity(), (left, _) -> left));

		return toUniqueKeyList(parameters).stream()
			.filter(parameter -> changes(existingByKey.get(parameter.getKey()), parameter))
			.map(Parameter::getKey)
			.toList();
	}

	/**
	 * Sent in key when the values of it differ from how they stand on the errand, and nothing when they do not. Only the
	 * values are compared.
	 *
	 * @param  entity errand the parameter belongs to
	 * @param  key    parameter being written
	 * @param  values values of the request
	 * @return        the key when the request would change it, empty otherwise
	 */
	public static List<String> changedValueKeys(final ErrandEntity entity, final String key, final List<String> values) {
		return Optional.ofNullable(entity.getParameters()).orElse(emptyList()).stream()
			.filter(parameter -> Objects.equals(parameter.getKey(), key))
			.findFirst()
			.filter(parameter -> Objects.equals(Optional.ofNullable(parameter.getValues()).orElse(emptyList()), Optional.ofNullable(values).orElse(emptyList())))
			.map(_ -> List.<String>of())
			.orElse(List.of(key));
	}

	private static boolean changes(final ParameterEntity existing, final Parameter incoming) {
		return isNull(existing)
			|| !Objects.equals(existing.getDisplayName(), incoming.getDisplayName())
			|| !Objects.equals(existing.getParameterGroup(), incoming.getGroup())
			|| !Objects.equals(Optional.ofNullable(existing.getValues()).orElse(emptyList()), Optional.ofNullable(incoming.getValues()).orElse(emptyList()));
	}

	public static List<Parameter> toParameterList(final List<ParameterEntity> parameters) {
		return Optional.ofNullable(parameters).orElse(emptyList()).stream()
			.map(ErrandParameterMapper::toParameter)
			.toList();
	}

	/**
	 * Parameters one per key, with the values of every parameter sent for it. Keys are trimmed before they are compared,
	 * and the display name and group are those of the first parameter sent for a key. The sent parameters are left as they
	 * were.
	 *
	 * @param  parameters parameters of the request
	 * @return            one parameter per trimmed key, ordered by key regardless of case, and keys that differ only in
	 *                    case in their natural order
	 */
	public static List<Parameter> toTrimmedUniqueKeyList(final List<Parameter> parameters) {
		final var trimmed = Optional.ofNullable(parameters).orElse(emptyList()).stream()
			.map(parameter -> Parameter.create()
				.withKey(parameter.getKey().trim())
				.withDisplayName(parameter.getDisplayName())
				.withGroup(parameter.getGroup())
				.withValues(parameter.getValues()))
			.toList();

		return toUniqueKeyList(trimmed).stream()
			.sorted(KEY_ORDER)
			.toList();
	}

	/**
	 * Maps parameters to entities of a handling artefact, one per key with the values of every parameter sent for it, in
	 * the order of the keys. Keys are trimmed before they are compared, and the display name and group are those of the
	 * first parameter sent for a key.
	 *
	 * @param  <E>        the type of the entities.
	 * @param  parameters the parameters sent.
	 * @param  factory    creates an entity that already points at its artefact.
	 * @return            the entities, in a list that may be changed.
	 */
	public static <E extends ArtefactParameter> List<E> toArtefactParameterEntities(final List<Parameter> parameters, final Supplier<E> factory) {
		return new ArrayList<>(toTrimmedUniqueKeyList(parameters).stream()
			.map(parameter -> {
				final var entity = factory.get();
				entity.setKey(parameter.getKey());
				entity.setDisplayName(parameter.getDisplayName());
				entity.setParameterGroup(parameter.getGroup());
				entity.setValues(parameter.getValues());
				return entity;
			})
			.toList());
	}

	public static Parameter toArtefactParameter(final ArtefactParameter entity) {
		return Optional.ofNullable(entity)
			.map(e -> Parameter.create()
				.withKey(e.getKey())
				.withDisplayName(e.getDisplayName())
				.withGroup(e.getParameterGroup())
				.withValues(e.getValues()))
			.orElse(null);
	}

	/**
	 * Maps the parameters of a handling artefact in the order of their keys, the same order whatever order the database
	 * reads them in.
	 */
	public static List<Parameter> toArtefactParameters(final List<? extends ArtefactParameter> entities) {
		return Optional.ofNullable(entities).orElse(emptyList()).stream()
			.map(ErrandParameterMapper::toArtefactParameter)
			.sorted(KEY_ORDER)
			.toList();
	}

	/**
	 * Replaces the parameters of a handling artefact in place. Replacements that come out the same as the stored
	 * parameters, in whatever order they were sent, leave the stored ones untouched.
	 *
	 * @param  <E>          the type of the entities.
	 * @param  stored       the parameters of the artefact, or null when it has none.
	 * @param  setter       gives the artefact a list of parameters, called only when it has none.
	 * @param  replacements the parameters to put in place of the stored ones.
	 * @return              true when the parameters were replaced, for the caller to mark the artefact modified.
	 */
	public static <E extends ArtefactParameter> boolean replaceArtefactParameters(final List<E> stored, final Consumer<List<E>> setter, final List<E> replacements) {
		if (toArtefactParameters(replacements).equals(toArtefactParameters(stored))) {
			return false;
		}

		if (isNull(stored)) {
			setter.accept(new ArrayList<>(replacements));
			return true;
		}

		stored.clear();
		stored.addAll(replacements);
		return true;
	}

	public static List<Parameter> toUniqueKeyList(List<Parameter> parameterList) {
		return Optional.ofNullable(parameterList).orElse(emptyList()).stream()
			.collect(groupingBy(Parameter::getKey, LinkedHashMap::new, Collectors.toList()))
			.entrySet()
			.stream()
			.map(entry -> Parameter.create()
				.withDisplayName(entry.getValue().getFirst().getDisplayName())
				.withGroup(entry.getValue().getFirst().getGroup())
				.withKey(entry.getKey())
				.withValues(new ArrayList<>(entry.getValue().stream()
					.map(Parameter::getValues)
					.filter(Objects::nonNull)
					.flatMap(List::stream)
					.toList())))
			.toList();
	}
}

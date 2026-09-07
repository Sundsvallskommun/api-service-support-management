package se.sundsvall.supportmanagement.service.mapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public final class ErrandParameterMapper {

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
	 * Replaces the parameters of the errand with sent in ones, leaving keys the caller may not reach untouched.
	 * <p>
	 * The merge deletes every key absent from the request, so without that guard a caller restricted to a few keys would
	 * silently delete the parameters they are not even allowed to see, simply by patching back the list they were served.
	 *
	 * @param entity        errand to merge into
	 * @param parameters    parameters replacing the reachable ones
	 * @param accessibleKey predicate accepting the keys the caller may reach
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
	 * Sent in key when the values of it differ from how they stand on the errand, and nothing when they do not. The
	 * endpoint writing a single parameter only ever writes its values, so nothing else is compared.
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

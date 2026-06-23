package se.sundsvall.supportmanagement.service.mapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;

import static java.util.Collections.emptyList;
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
		if (entity.getParameters() == null) {
			entity.setParameters(new ArrayList<>());
		}
		final var existing = entity.getParameters();
		final var uniqueIncoming = toUniqueKeyList(parameters);
		final var incomingByKey = uniqueIncoming.stream().collect(toMap(Parameter::getKey, identity()));
		final var existingByKey = existing.stream().collect(toMap(ParameterEntity::getKey, identity()));

		existing.removeIf(e -> !incomingByKey.containsKey(e.getKey()));
		existing.forEach(e -> {
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

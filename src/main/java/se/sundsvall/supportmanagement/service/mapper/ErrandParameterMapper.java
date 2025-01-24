package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;

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
			.withKey(parameter.getKey())
			.withValues(parameter.getValues());
	}

	public static Parameter toParameter(final ParameterEntity parameter) {
		return Parameter.create()
			.withDisplayName(parameter.getDisplayName())
			.withKey(parameter.getKey())
			.withValues(parameter.getValues());
	}

	public static List<Parameter> toParameterList(final List<ParameterEntity> parameters) {
		return Optional.ofNullable(parameters).orElse(emptyList()).stream()
			.map(ErrandParameterMapper::toParameter)
			.toList();
	}

	public static List<Parameter> toUniqueKeyList(List<Parameter> parameterList) {
		// TODO: Add mapping for group
		return new ArrayList<>(Optional.ofNullable(parameterList).orElse(emptyList()).stream()
			.collect(groupingBy(Parameter::getKey))
			.entrySet()
			.stream()
			.map(entry -> Parameter.create()
				.withDisplayName(entry.getValue().getFirst().getDisplayName())
				.withKey(entry.getKey())
				.withValues(new ArrayList<>(entry.getValue().stream()
					.map(Parameter::getValues)
					.filter(Objects::nonNull)
					.flatMap(List::stream)
					.toList())))
			.toList());
	}
}

package se.sundsvall.supportmanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderParameterEntity;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;

public final class StakeholderParameterMapper {

	private StakeholderParameterMapper() {
		// Intentionally empty
	}

	public static List<StakeholderParameterEntity> toStakeholderParameterEntityList(final List<Parameter> parameters, StakeholderEntity entity) {
		return new ArrayList<>(toUniqueKeyList(parameters).stream()
			.map(parameter -> toStakeholderParameterEntity(parameter).withStakeholderEntity(entity))
			.toList());
	}

	public static StakeholderParameterEntity toStakeholderParameterEntity(final Parameter parameter) {
		return StakeholderParameterEntity.create()
			.withDisplayName(parameter.getDisplayName())
			.withKey(parameter.getKey())
			.withValues(parameter.getValues());
	}

	public static Parameter toParameter(final StakeholderParameterEntity parameter) {
		return Parameter.create()
			.withDisplayName(parameter.getDisplayName())
			.withKey(parameter.getKey())
			.withValues(parameter.getValues());
	}

	public static List<Parameter> toParameterList(final List<StakeholderParameterEntity> parameters) {
		return Optional.ofNullable(parameters).orElse(emptyList()).stream()
			.map(StakeholderParameterMapper::toParameter)
			.toList();
	}

	public static List<Parameter> toUniqueKeyList(List<Parameter> parameterList) {
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

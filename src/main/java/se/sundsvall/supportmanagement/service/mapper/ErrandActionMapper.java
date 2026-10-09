package se.sundsvall.supportmanagement.service.mapper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import se.sundsvall.supportmanagement.api.model.config.action.Config;
import se.sundsvall.supportmanagement.api.model.config.action.Parameter;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigConditionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigKeyValues;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.OperationType;

public class ErrandActionMapper {

	private ErrandActionMapper() {}

	public static Config toActionConfig(ActionConfigEntity entity) {
		return Config.create()
			.withActive(entity.getActive())
			.withId(entity.getId())
			.withParameters(entity.getParameters().stream().map(ErrandActionMapper::toParameter).toList())
			.withConditions(entity.getConditions().stream().map(ErrandActionMapper::toParameter).toList())
			.withName(entity.getName())
			.withDisplayValue(entity.getDisplayValue())
			.withOperationTypes(List.copyOf(entity.getOperationTypes()));
	}

	public static Parameter toParameter(ActionConfigKeyValues actionConfigKeyValues) {
		return Parameter.create()
			.withKey(actionConfigKeyValues.getKey())
			.withValues(actionConfigKeyValues.getValues());
	}

	public static ActionConfigEntity toEntity(String municipalityId, String namespace, Config config) {
		final var entity = ActionConfigEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withActive(config.getActive())
			.withName(config.getName())
			.withDisplayValue(config.getDisplayValue())
			.withOperationTypes(toOperationTypes(config));

		entity.setConditions(config.getConditions().stream()
			.map(parameter -> ActionConfigConditionEntity.create()
				.withKey(parameter.getKey())
				.withValues(parameter.getValues())
				.withActionConfigEntity(entity))
			.toList());

		entity.setParameters(config.getParameters().stream()
			.map(parameter -> ActionConfigParameterEntity.create()
				.withKey(parameter.getKey())
				.withValues(parameter.getValues())
				.withActionConfigEntity(entity))
			.toList());

		return entity;
	}

	public static ActionConfigEntity updateEntity(ActionConfigEntity entity, Config config) {
		entity.setActive(config.getActive());
		entity.setName(config.getName());
		entity.setDisplayValue(config.getDisplayValue());
		entity.setOperationTypes(toOperationTypes(config));

		entity.getConditions().clear();
		entity.getConditions().addAll(config.getConditions().stream()
			.map(parameter -> ActionConfigConditionEntity.create()
				.withKey(parameter.getKey())
				.withValues(parameter.getValues())
				.withActionConfigEntity(entity))
			.toList());

		entity.getParameters().clear();
		entity.getParameters().addAll(config.getParameters().stream()
			.map(parameter -> ActionConfigParameterEntity.create()
				.withKey(parameter.getKey())
				.withValues(parameter.getValues())
				.withActionConfigEntity(entity))
			.toList());

		return entity;
	}

	/**
	 * An absent list is stored as an empty set, which means every operation the action supports.
	 */
	private static Set<OperationType> toOperationTypes(Config config) {
		return new LinkedHashSet<>(Optional.ofNullable(config.getOperationTypes()).orElse(List.of()));
	}

	public static Map<String, List<String>> toMap(List<Parameter> parameters) {
		return parameters.stream().collect(Collectors.toMap(Parameter::getKey, Parameter::getValues));
	}
}

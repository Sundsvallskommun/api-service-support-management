package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;

public final class ErrandParameterMapper {

	private ErrandParameterMapper() {
		//Intentionally empty
	}

	public static Map<String, ParameterEntity> toErrandParameterEntityMap(final Map<String, List<String>> parameters, ErrandEntity entity) {
		return ofNullable(parameters).orElse(emptyMap())
			.entrySet()
			.stream()
			.collect(toMap(Map.Entry::getKey, e -> toErrandParameterEntity(e.getValue()).withErrandEntity(entity)));
	}

	public static ParameterEntity toErrandParameterEntity(final List<String> errandParameter) {
		return ParameterEntity.create()
			.withValues(errandParameter);
	}

	public static Map<String, List<String>> toParameterMap(final Map<String, ParameterEntity> parameters) {
		return Optional.ofNullable(parameters).orElse(emptyMap())
			.entrySet().stream()
			.map(entry -> Map.entry(entry.getKey(), entry.getValue().getValues()))
			.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

}

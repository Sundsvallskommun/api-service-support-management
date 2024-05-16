package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Optional;

import se.sundsvall.supportmanagement.api.model.parameter.ErrandParameter;
import se.sundsvall.supportmanagement.api.model.parameter.ErrandParameters;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;

public final class ErrandParameterMapper {

	public static ErrandParameter toErrandParameter(final ParameterEntity parameterEntity) {
		return Optional.ofNullable(parameterEntity).map(entity -> ErrandParameter.create()
				.withId(entity.getId())
				.withName(entity.getName())
				.withValue(entity.getValue()))
			.orElse(null);
	}

	public static ErrandParameters toErrandParameters(final List<ParameterEntity> parameterEntities) {
		var errandParameters = Optional.ofNullable(parameterEntities).orElse(emptyList()).stream()
			.map(ErrandParameterMapper::toErrandParameter)
			.toList();

		return ErrandParameters.create()
			.withErrandParameters(errandParameters);
	}
}

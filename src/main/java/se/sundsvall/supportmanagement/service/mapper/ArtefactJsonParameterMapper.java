package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.model.AbstractArtefactJsonParameterEntity;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toJsonNode;

public final class ArtefactJsonParameterMapper {

	private ArtefactJsonParameterMapper() {}

	public static JsonParameter toJsonParameter(final AbstractArtefactJsonParameterEntity<?> entity) {
		return JsonParameter.create()
			.withKey(entity.getKey())
			.withSchemaId(entity.getSchemaId())
			.withValue(toJsonNode(entity.getValue()))
			.withVersion(entity.getVersion());
	}

	public static List<JsonParameter> toJsonParameters(final List<? extends AbstractArtefactJsonParameterEntity<?>> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ArtefactJsonParameterMapper::toJsonParameter)
			.toList();
	}
}

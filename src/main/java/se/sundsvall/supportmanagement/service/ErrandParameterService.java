package se.sundsvall.supportmanagement.service;

import static java.util.Collections.emptyList;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toErrandParameterEntityList;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toParameter;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toParameterList;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

@Service
public class ErrandParameterService {

	private static final String ERRAND_ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";

	private static final String PARAMETER_NOT_FOUND = "A parameter with key '%s' could not be found in errand with id '%s'";

	private final ErrandsRepository errandsRepository;

	public ErrandParameterService(final ErrandsRepository errandsRepository) {
		this.errandsRepository = errandsRepository;
	}

	public List<Parameter> updateErrandParameters(final String namespace, final String municipalityId, final String errandId, final List<Parameter> parameters) {
		final var errandEntity = findExistingErrand(errandId, namespace, municipalityId);

		if (errandEntity.getParameters() != null) {
			errandEntity.getParameters().clear();
		}

		errandEntity.getParameters().addAll(toErrandParameterEntityList(parameters, errandEntity));

		return toParameterList(errandsRepository.save(errandEntity).getParameters());
	}

	public List<String> readErrandParameter(final String namespace, final String municipalityId, final String errandId, final String parameterKey) {
		final var errand = findExistingErrand(errandId, namespace, municipalityId);
		return findParameterEntityOrElseThrow(errand, parameterKey);
	}

	public List<Parameter> findErrandParameters(final String namespace, final String municipalityId, final String errandId) {
		final var errandEntity = findExistingErrand(errandId, namespace, municipalityId);

		return toParameterList(errandEntity.getParameters());
	}

	public Parameter updateErrandParameter(final String namespace, final String municipalityId, final String errandId, final String parameterKey, final List<String> parameterValues) {
		final var errandEntity = findExistingErrand(errandId, namespace, municipalityId);

		final var parameterEntity = errandEntity.getParameters().stream()
			.filter(paramEntity -> Objects.equals(paramEntity.getKey(), parameterKey))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(PARAMETER_NOT_FOUND, parameterKey, errandEntity.getId())))
			.withValues(parameterValues);

		errandsRepository.save(errandEntity);

		return toParameter(parameterEntity);
	}

	public void deleteErrandParameter(final String namespace, final String municipalityId, final String errandId, final String parameterKey) {
		final var errandEntity = findExistingErrand(errandId, namespace, municipalityId);

		if (errandEntity.getParameters() == null) {
			return;
		}

		final var parameterToRemove = errandEntity.getParameters().stream()
			.filter(paramEntity -> Objects.equals(paramEntity.getKey(), parameterKey))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(PARAMETER_NOT_FOUND, parameterKey, errandEntity.getId())));

		errandEntity.getParameters().remove(parameterToRemove);

		errandsRepository.save(errandEntity);
	}

	ErrandEntity findExistingErrand(final String id, final String namespace, final String municipalityId) {
		return errandsRepository.findByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, id, namespace, municipalityId)));
	}

	List<String> findParameterEntityOrElseThrow(final ErrandEntity errandEntity, final String parameterKey) {
		return Optional.ofNullable(errandEntity.getParameters()).orElse(emptyList()).stream()
			.filter(paramEntity -> Objects.equals(paramEntity.getKey(), parameterKey))
			.findAny()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(PARAMETER_NOT_FOUND, parameterKey, errandEntity.getId())))
			.getValues();
	}
}

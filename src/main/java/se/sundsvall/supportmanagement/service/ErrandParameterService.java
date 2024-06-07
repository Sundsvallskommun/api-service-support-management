package se.sundsvall.supportmanagement.service;

import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toErrandParameterEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toParameterMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

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

	public Map<String, List<String>> updateErrandParameters(final String namespace, final String municipalityId, final String errandId, final Map<String, List<String>> errandParameter) {
		final var errand = findExistingErrand(errandId, namespace, municipalityId);

		if (errand.getParameters() == null) {
			errand.setParameters(new HashMap<>());
		}

		errandParameter.forEach((key, value) -> errand.getParameters().put(key, toErrandParameterEntity(value).withErrandEntity(errand)));

		return toParameterMap(errandsRepository.save(errand).getParameters());
	}

	public List<String> readErrandParameter(final String namespace, final String municipalityId, final String errandId, final String parameterKey) {
		final var errand = findExistingErrand(errandId, namespace, municipalityId);
		return findParameterEntityOrElseThrow(errand, parameterKey);
	}

	public Map<String, List<String>> findErrandParameters(final String namespace, final String municipalityId, final String errandId) {
		final var errand = findExistingErrand(errandId, namespace, municipalityId);

		return toParameterMap(errand.getParameters());
	}

	public List<String> updateErrandParameter(final String namespace, final String municipalityId, final String errandId, final String parameterKey, final List<String> errandParameter) {
		final var errand = findExistingErrand(errandId, namespace, municipalityId);

		final var parameter = toErrandParameterEntity(errandParameter);

		errand.getParameters().get(parameterKey).setValues(parameter.getValues());

		return toParameterMap(errandsRepository.save(errand).getParameters()).get(parameterKey);
	}


	public void deleteErrandParameter(final String namespace, final String municipalityId, final String errandId, final String parameterKey) {
		final var errand = findExistingErrand(errandId, namespace, municipalityId);
		errand.getParameters().remove(parameterKey);
		errandsRepository.save(errand);
	}

	ErrandEntity findExistingErrand(final String id, final String namespace, final String municipalityId) {
		return errandsRepository.findByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, id, namespace, municipalityId)));
	}

	List<String> findParameterEntityOrElseThrow(final ErrandEntity errandEntity, final String parameterKey) {
		return Optional.ofNullable(errandEntity.getParameters().get(parameterKey))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(PARAMETER_NOT_FOUND, parameterKey, errandEntity.getId())))
			.getValues();
	}


}

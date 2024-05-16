package se.sundsvall.supportmanagement.service;

import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toErrandParameter;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toErrandParameters;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.model.parameter.ErrandParameter;
import se.sundsvall.supportmanagement.api.model.parameter.ErrandParameters;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.ParameterRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;

@Service
public class ErrandParameterService {

	private static final String ERRAND_ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final String PARAMETER_NOT_FOUND = "A parameter with id '%s' could not be found in errand with id '%s'";

	private final ErrandsRepository errandsRepository;
	private final ParameterRepository parameterRepository;

	public ErrandParameterService(final ErrandsRepository errandsRepository, final ParameterRepository parameterRepository) {
		this.errandsRepository = errandsRepository;
		this.parameterRepository = parameterRepository;
	}

	public String createErrandParameter(final String namespace, final String municipalityId, final String errandId, final ErrandParameter errandParameter) {
		var errand = findExistingErrand(errandId, namespace, municipalityId);

		var parameter = ParameterEntity.create()
			.withName(errandParameter.getName())
			.withValue(errandParameter.getValue());

		parameterRepository.save(parameter);
		Optional.ofNullable(errand.getParameters()).ifPresentOrElse(parameters -> parameters.add(parameter),
			() -> errand.setParameters(List.of(parameter)));
		errandsRepository.save(errand);

		return parameter.getId();
	}

	public ErrandParameter readErrandParameter(final String namespace, final String municipalityId, final String errandId, final String parameterId) {
		var errand = findExistingErrand(errandId, namespace, municipalityId);
		var parameter = findParameterEntityOrElseThrow(errand, parameterId);

		return toErrandParameter(parameter);
	}

	public ErrandParameters findErrandParameters(final String namespace, final String municipalityId, final String errandId) {
		var errand = findExistingErrand(errandId, namespace, municipalityId);

		return toErrandParameters(errand.getParameters());
	}

	public ErrandParameter updateErrandParameter(final String namespace, final String municipalityId, final String errandId, final String parameterId, final ErrandParameter errandParameter) {
		var errand = findExistingErrand(errandId, namespace, municipalityId);

		var parameterEntity = findParameterEntityOrElseThrow(errand, parameterId);

		parameterEntity.setName(errandParameter.getName());
		parameterEntity.setValue(errandParameter.getValue());

		return toErrandParameter(parameterRepository.save(parameterEntity));
	}

	public void deleteErrandParameter(final String namespace, final String municipalityId, final String errandId, final String parameterId) {
		var errand = findExistingErrand(errandId, namespace, municipalityId);

		var parameter = findParameterEntityOrElseThrow(errand, parameterId);

		errand.getParameters().remove(parameter);

		errandsRepository.save(errand);
		parameterRepository.delete(parameter);
	}

	ErrandEntity findExistingErrand(String id, String namespace, String municipalityId) {
		return errandsRepository.findByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, id, namespace, municipalityId)));
	}

	ParameterEntity findParameterEntityOrElseThrow(final ErrandEntity errandEntity, final String parameterId) {
		return errandEntity.getParameters().stream()
			.filter(p -> parameterId.equals(p.getId()))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(PARAMETER_NOT_FOUND, parameterId, errandEntity.getId())));
	}
}

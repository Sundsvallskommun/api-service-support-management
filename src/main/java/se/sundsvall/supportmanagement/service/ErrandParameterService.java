package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.Collections.emptyList;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toErrandParameterEntityList;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toParameter;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toParameterList;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

@Service
@Transactional
public class ErrandParameterService {

	private static final String PARAMETER_NOT_FOUND = "A parameter with key '%s' could not be found in errand with id '%s'";

	private final ErrandsRepository errandsRepository;
	private final AccessControlService accessControlService;

	public ErrandParameterService(final ErrandsRepository errandsRepository, final AccessControlService accessControlService) {
		this.errandsRepository = errandsRepository;
		this.accessControlService = accessControlService;
	}

	public List<Parameter> updateErrandParameters(final String namespace, final String municipalityId, final String errandId, final List<Parameter> parameters) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, RW);

		if (errandEntity.getParameters() != null) {
			errandEntity.getParameters().clear();
		}

		errandEntity.getParameters().addAll(toErrandParameterEntityList(parameters, errandEntity));

		return toParameterList(errandsRepository.save(errandEntity).getParameters());
	}

	public List<String> readErrandParameter(final String namespace, final String municipalityId, final String errandId, final String parameterKey) {
		final var errand = accessControlService.getErrand(namespace, municipalityId, errandId, false, R, RW);
		return findParameterEntityOrElseThrow(errand, parameterKey);
	}

	public List<Parameter> findErrandParameters(final String namespace, final String municipalityId, final String errandId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, R, RW);
		return toParameterList(errandEntity.getParameters());
	}

	public Parameter updateErrandParameter(final String namespace, final String municipalityId, final String errandId, final String parameterKey, final List<String> parameterValues) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, RW);

		final var parameterEntity = errandEntity.getParameters().stream()
			.filter(paramEntity -> Objects.equals(paramEntity.getKey(), parameterKey))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(PARAMETER_NOT_FOUND, parameterKey, errandEntity.getId())))
			.withValues(parameterValues);

		errandsRepository.save(errandEntity);

		return toParameter(parameterEntity);
	}

	public void deleteErrandParameter(final String namespace, final String municipalityId, final String errandId, final String parameterKey) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, RW);

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

	List<String> findParameterEntityOrElseThrow(final ErrandEntity errandEntity, final String parameterKey) {
		return Optional.ofNullable(errandEntity.getParameters()).orElse(emptyList()).stream()
			.filter(paramEntity -> Objects.equals(paramEntity.getKey(), parameterKey))
			.findAny()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(PARAMETER_NOT_FOUND, parameterKey, errandEntity.getId())))
			.getValues();
	}
}

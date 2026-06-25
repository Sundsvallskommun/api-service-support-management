package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.mergeParameters;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toParameter;
import static se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper.toParameterList;
import static se.sundsvall.supportmanagement.service.util.ETagUtil.validateIfMatch;

@Service
public class ErrandParameterService {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandParameterService.class);
	private static final String PARAMETER_NOT_FOUND = "A parameter with key '%s' could not be found in errand with id '%s'";

	private final ErrandsRepository errandsRepository;
	private final AccessControlService accessControlService;
	private final EntityManager entityManager;

	ErrandParameterService(final ErrandsRepository errandsRepository, final AccessControlService accessControlService, final EntityManager entityManager) {
		this.errandsRepository = errandsRepository;
		this.accessControlService = accessControlService;
		this.entityManager = entityManager;
	}

	@Transactional
	public List<Parameter> updateErrandParameters(final String namespace, final String municipalityId, final String errandId, final String ifMatch, final List<Parameter> parameters) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, RW);

		if (ifMatch == null) {
			LOG.debug("PATCH /errands/{}/parameters received without If-Match header (namespace={}, municipalityId={})", sanitizeForLogging(errandId), sanitizeForLogging(namespace), sanitizeForLogging(municipalityId));
		}
		validateIfMatch(ifMatch, errandEntity.getVersion());
		entityManager.lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		mergeParameters(errandEntity, parameters);

		return toParameterList(errandsRepository.save(errandEntity).getParameters());
	}

	@Transactional(readOnly = true)
	public Parameter readErrandParameter(final String namespace, final String municipalityId, final String errandId, final String parameterKey) {
		final var errand = accessControlService.getErrand(namespace, municipalityId, errandId, false, R, RW);
		return toParameter(findParameterEntityOrElseThrow(errand, parameterKey));
	}

	@Transactional(readOnly = true)
	public List<Parameter> findErrandParameters(final String namespace, final String municipalityId, final String errandId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, R, RW);
		return toParameterList(errandEntity.getParameters());
	}

	@Transactional
	public Parameter updateErrandParameter(final String namespace, final String municipalityId, final String errandId, final String parameterKey, final String ifMatch, final List<String> parameterValues) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, RW);

		final var parameterEntity = errandEntity.getParameters().stream()
			.filter(paramEntity -> Objects.equals(paramEntity.getKey(), parameterKey))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(PARAMETER_NOT_FOUND, parameterKey, errandEntity.getId())));

		if (ifMatch == null) {
			LOG.debug("PATCH /errands/{}/parameters/{} received without If-Match header (namespace={}, municipalityId={})", sanitizeForLogging(errandId), sanitizeForLogging(parameterKey), sanitizeForLogging(namespace), sanitizeForLogging(municipalityId));
		}
		validateIfMatch(ifMatch, parameterEntity.getVersion());
		entityManager.lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		parameterEntity.withValues(parameterValues);
		errandsRepository.save(errandEntity);
		return toParameter(parameterEntity);
	}

	@Transactional
	public void deleteErrandParameter(final String namespace, final String municipalityId, final String errandId, final String parameterKey, final String ifMatch) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, RW);

		if (errandEntity.getParameters() == null) {
			return;
		}

		final var parameterToRemove = errandEntity.getParameters().stream()
			.filter(paramEntity -> Objects.equals(paramEntity.getKey(), parameterKey))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(PARAMETER_NOT_FOUND, parameterKey, errandEntity.getId())));

		if (ifMatch == null) {
			LOG.debug("DELETE /errands/{}/parameters/{} received without If-Match header (namespace={}, municipalityId={})", sanitizeForLogging(errandId), sanitizeForLogging(parameterKey), sanitizeForLogging(namespace), sanitizeForLogging(municipalityId));
		}
		validateIfMatch(ifMatch, parameterToRemove.getVersion());
		entityManager.lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		errandEntity.getParameters().remove(parameterToRemove);
		errandsRepository.save(errandEntity);
	}

	ParameterEntity findParameterEntityOrElseThrow(final ErrandEntity errandEntity, final String parameterKey) {
		return Optional.ofNullable(errandEntity.getParameters()).orElse(emptyList()).stream()
			.filter(paramEntity -> Objects.equals(paramEntity.getKey(), parameterKey))
			.findAny()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(PARAMETER_NOT_FOUND, parameterKey, errandEntity.getId())));
	}
}

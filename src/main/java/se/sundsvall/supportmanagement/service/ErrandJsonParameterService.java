package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toJsonParameter;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toJsonString;
import static se.sundsvall.supportmanagement.service.util.ETagUtil.validateIfMatch;

@Service
public class ErrandJsonParameterService {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandJsonParameterService.class);
	private static final String JSON_PARAMETER_NOT_FOUND = "A JSON parameter with key '%s' could not be found in errand with id '%s'";

	private final ErrandsRepository errandsRepository;
	private final AccessControlService accessControlService;
	private final EntityManager entityManager;

	ErrandJsonParameterService(final ErrandsRepository errandsRepository, final AccessControlService accessControlService, final EntityManager entityManager) {
		this.errandsRepository = errandsRepository;
		this.accessControlService = accessControlService;
		this.entityManager = entityManager;
	}

	@Transactional(readOnly = true)
	public JsonParameter readJsonParameter(final String namespace, final String municipalityId, final String errandId, final String key) {
		final var errand = accessControlService.getErrand(namespace, municipalityId, errandId, false, R, RW);
		return toJsonParameter(findJsonParameterEntityOrElseThrow(errand, key));
	}

	@Transactional
	public JsonParameter updateJsonParameter(final String namespace, final String municipalityId, final String errandId, final String key, final String ifMatch, final JsonParameter jsonParameter) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, RW);

		final var existing = Optional.ofNullable(errandEntity.getJsonParameters())
			.flatMap(list -> list.stream().filter(e -> Objects.equals(e.getKey(), key)).findFirst());

		if (ifMatch == null) {
			LOG.debug("PUT /errands/{}/json-parameters/{} received without If-Match header (namespace={}, municipalityId={})", errandId, key, namespace, municipalityId);
		}
		existing.ifPresent(e -> validateIfMatch(ifMatch, e.getVersion()));
		entityManager.lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		final JsonParameterEntity entity;
		if (existing.isPresent()) {
			entity = existing.get();
			entity.setSchemaId(jsonParameter.getSchemaId());
			entity.setValue(toJsonString(jsonParameter.getValue()));
		} else {
			if (errandEntity.getJsonParameters() == null) {
				errandEntity.setJsonParameters(new ArrayList<>());
			}
			entity = JsonParameterEntity.create()
				.withErrandEntity(errandEntity)
				.withKey(key)
				.withSchemaId(jsonParameter.getSchemaId())
				.withValue(toJsonString(jsonParameter.getValue()));
			errandEntity.getJsonParameters().add(entity);
		}

		errandsRepository.save(errandEntity);
		return toJsonParameter(entity);
	}

	@Transactional
	public void deleteJsonParameter(final String namespace, final String municipalityId, final String errandId, final String key, final String ifMatch) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, RW);

		final var entityToRemove = findJsonParameterEntityOrElseThrow(errandEntity, key);

		if (ifMatch == null) {
			LOG.debug("DELETE /errands/{}/json-parameters/{} received without If-Match header (namespace={}, municipalityId={})", errandId, key, namespace, municipalityId);
		}
		validateIfMatch(ifMatch, entityToRemove.getVersion());
		entityManager.lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		errandEntity.getJsonParameters().remove(entityToRemove);
		errandsRepository.save(errandEntity);
	}

	JsonParameterEntity findJsonParameterEntityOrElseThrow(final ErrandEntity errandEntity, final String key) {
		return Optional.ofNullable(errandEntity.getJsonParameters()).stream()
			.flatMap(java.util.List::stream)
			.filter(e -> Objects.equals(e.getKey(), key))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(JSON_PARAMETER_NOT_FOUND, key, errandEntity.getId())));
	}
}

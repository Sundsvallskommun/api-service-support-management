package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MeasureTypeRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMeasureMapper.toMeasure;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMeasureMapper.toMeasureEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMeasureMapper.toMeasures;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMeasureMapper.updateMeasureEntity;

@Service
public class ErrandMeasureService {

	private static final String MEASURE_NOT_FOUND = "A measure with id '%s' could not be found in errand with id '%s'";
	private static final String BAD_MEASURE_TYPE = "'%s' is not a valid measure type for namespace '%s' and municipality with id '%s'";
	private static final String BAD_ROLE = "'%s' is not a valid role for namespace '%s' and municipality with id '%s'";

	private final ErrandsRepository errandsRepository;
	private final MeasureTypeRepository measureTypeRepository;
	private final RoleRepository roleRepository;
	private final AccessControlService accessControlService;
	private final EntityManager entityManager;

	ErrandMeasureService(final ErrandsRepository errandsRepository, final MeasureTypeRepository measureTypeRepository, final RoleRepository roleRepository, final AccessControlService accessControlService, final EntityManager entityManager) {
		this.errandsRepository = errandsRepository;
		this.measureTypeRepository = measureTypeRepository;
		this.roleRepository = roleRepository;
		this.accessControlService = accessControlService;
		this.entityManager = entityManager;
	}

	@Transactional
	public String createErrandMeasure(final String namespace, final String municipalityId, final String errandId, final Measure measure) {
		validateMeasureType(measure.getType(), namespace, municipalityId);
		validateRole(measure.getAddedByRole(), namespace, municipalityId);

		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		entityManager.lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		final var measureEntity = toMeasureEntity(measure, errandEntity);
		ofNullable(errandEntity.getMeasures()).orElseGet(() -> {
			errandEntity.setMeasures(new ArrayList<>());
			return errandEntity.getMeasures();
		}).add(measureEntity);

		entityManager.persist(measureEntity);
		errandsRepository.save(errandEntity);
		return measureEntity.getId();
	}

	@Transactional(readOnly = true)
	public Measure readErrandMeasure(final String namespace, final String municipalityId, final String errandId, final String measureId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, ProtectedResource.MEASURE, LR);
		return toMeasure(findMeasureEntityOrElseThrow(errandEntity, measureId));
	}

	@Transactional(readOnly = true)
	public List<Measure> findErrandMeasures(final String namespace, final String municipalityId, final String errandId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, ProtectedResource.MEASURE, LR);
		return toMeasures(errandEntity.getMeasures());
	}

	@Transactional
	public Measure updateErrandMeasure(final String namespace, final String municipalityId, final String errandId, final String measureId, final Measure measure) {
		validateMeasureType(measure.getType(), namespace, municipalityId);
		validateRole(measure.getAddedByRole(), namespace, municipalityId);

		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		entityManager.lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		final var measureEntity = findMeasureEntityOrElseThrow(errandEntity, measureId);
		updateMeasureEntity(measureEntity, measure);

		errandsRepository.save(errandEntity);
		return toMeasure(measureEntity);
	}

	@Transactional
	public void deleteErrandMeasure(final String namespace, final String municipalityId, final String errandId, final String measureId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		entityManager.lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		final var measureEntity = findMeasureEntityOrElseThrow(errandEntity, measureId);
		ofNullable(errandEntity.getMeasures()).ifPresent(measures -> measures.remove(measureEntity));

		errandsRepository.save(errandEntity);
	}

	private void validateMeasureType(final String type, final String namespace, final String municipalityId) {
		ofNullable(type).ifPresent(t -> {
			if (!measureTypeRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, t)) {
				throw Problem.valueOf(BAD_REQUEST, BAD_MEASURE_TYPE.formatted(t, namespace, municipalityId));
			}
		});
	}

	private void validateRole(final String role, final String namespace, final String municipalityId) {
		ofNullable(role).ifPresent(r -> {
			if (!roleRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, r)) {
				throw Problem.valueOf(BAD_REQUEST, BAD_ROLE.formatted(r, namespace, municipalityId));
			}
		});
	}

	private MeasureEntity findMeasureEntityOrElseThrow(final ErrandEntity errandEntity, final String measureId) {
		return ofNullable(errandEntity.getMeasures()).orElse(emptyList()).stream()
			.filter(entity -> entity.getId().equals(measureId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(MEASURE_NOT_FOUND, measureId, errandEntity.getId())));
	}
}

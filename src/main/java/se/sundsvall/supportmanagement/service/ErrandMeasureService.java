package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachment;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachmentLink;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.integration.db.DecisionRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MeasureAttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.MeasureJsonParameterRepository;
import se.sundsvall.supportmanagement.integration.db.StatementRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.ArtefactAttachmentService.ArtefactLinks;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMeasureMapper.toMeasure;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMeasureMapper.toMeasureEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMeasureMapper.toMeasures;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMeasureMapper.updateMeasureEntity;
import static se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters.ownedParameterIds;
import static se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters.removeParameters;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getCallerIdentity;

@Service
public class ErrandMeasureService {

	private static final String DECISION_NOT_FOUND = "A decision with id '%s' could not be found in errand with id '%s'";
	private static final String STATEMENT_NOT_FOUND = "A statement with id '%s' could not be found in errand with id '%s'";
	private static final String MEASURE_NOT_FOUND = "A measure with id '%s' could not be found in errand with id '%s'";

	private final ErrandsRepository errandsRepository;
	private final MeasureValidator measureValidator;
	private final MeasureAttachmentRepository measureAttachmentRepository;
	private final MeasureJsonParameterRepository measureJsonParameterRepository;
	private final DecisionRepository decisionRepository;
	private final StatementRepository statementRepository;
	private final ArtefactAttachmentService artefactAttachmentService;
	private final ArtefactJsonParameterService artefactJsonParameterService;
	private final AccessControlService accessControlService;
	private final EntityManager entityManager;

	ErrandMeasureService(final ErrandsRepository errandsRepository, final MeasureValidator measureValidator, final MeasureAttachmentRepository measureAttachmentRepository,
		final MeasureJsonParameterRepository measureJsonParameterRepository, final DecisionRepository decisionRepository, final StatementRepository statementRepository,
		final ArtefactAttachmentService artefactAttachmentService, final ArtefactJsonParameterService artefactJsonParameterService, final AccessControlService accessControlService,
		final EntityManager entityManager) {
		this.errandsRepository = errandsRepository;
		this.measureValidator = measureValidator;
		this.measureAttachmentRepository = measureAttachmentRepository;
		this.measureJsonParameterRepository = measureJsonParameterRepository;
		this.decisionRepository = decisionRepository;
		this.statementRepository = statementRepository;
		this.artefactAttachmentService = artefactAttachmentService;
		this.artefactJsonParameterService = artefactJsonParameterService;
		this.accessControlService = accessControlService;
		this.entityManager = entityManager;
	}

	@Transactional
	public String createErrandMeasure(final String namespace, final String municipalityId, final String errandId, final Measure measure) {
		measureValidator.validate(measure, namespace, municipalityId);

		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		entityManager.lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		final var measureEntity = toMeasureEntity(measure, errandEntity)
			.withCreatedBy(getCallerIdentity());
		applyProvenance(namespace, municipalityId, errandId, measure, measureEntity);

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
		measureValidator.validate(measure, namespace, municipalityId);

		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		entityManager.lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		final var measureEntity = findMeasureEntityOrElseThrow(errandEntity, measureId);
		updateMeasureEntity(measureEntity, measure).setModifiedBy(getCallerIdentity());
		applyProvenance(namespace, municipalityId, errandId, measure, measureEntity);

		errandsRepository.save(errandEntity);
		return toMeasure(measureEntity);
	}

	@Transactional
	public void deleteErrandMeasure(final String namespace, final String municipalityId, final String errandId, final String measureId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		entityManager.lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		final var measureEntity = findMeasureEntityOrElseThrow(errandEntity, measureId);

		// Named now: the links that name them go with the measure.
		final var ownedParameters = ownedParameterIds(measureEntity.getJsonParameterLinks());

		// Flushed here rather than left to the commit, so that the two removals reach the database in this order.
		ofNullable(errandEntity.getMeasures()).ifPresent(measures -> measures.remove(measureEntity));
		errandsRepository.saveAndFlush(errandEntity);

		removeParameters(errandEntity, ownedParameters);
		errandsRepository.save(errandEntity);
	}

	@Transactional
	public String createMeasureAttachment(final String namespace, final String municipalityId, final String errandId, final String measureId, final MultipartFile file, final Integer sortOrder) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		final var entity = findMeasureEntityOrElseThrow(errandEntity, measureId);

		return artefactAttachmentService.uploadAndLink(namespace, municipalityId, errandId, file, sortOrder, artefactLinks(entity));
	}

	@Transactional
	public ArtefactAttachment linkMeasureAttachment(final String namespace, final String municipalityId, final String errandId, final String measureId, final String attachmentId,
		final ArtefactAttachmentLink link) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		final var entity = findMeasureEntityOrElseThrow(errandEntity, measureId);

		return artefactAttachmentService.link(namespace, municipalityId, errandId, attachmentId, link.getSortOrder(), artefactLinks(entity));
	}

	@Transactional
	public ArtefactAttachment updateMeasureAttachment(final String namespace, final String municipalityId, final String errandId, final String measureId, final String attachmentId,
		final ArtefactAttachmentLink link) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		final var entity = findMeasureEntityOrElseThrow(errandEntity, measureId);

		return artefactAttachmentService.update(attachmentId, link, attachmentLinks(entity));
	}

	@Transactional
	public void unlinkMeasureAttachment(final String namespace, final String municipalityId, final String errandId, final String measureId, final String attachmentId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		final var entity = findMeasureEntityOrElseThrow(errandEntity, measureId);

		artefactAttachmentService.unlink(attachmentId, attachmentLinks(entity));
		errandsRepository.saveAndFlush(errandEntity);
	}

	@Transactional(readOnly = true)
	public List<JsonParameter> readMeasureJsonParameters(final String namespace, final String municipalityId, final String errandId, final String measureId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, ProtectedResource.MEASURE, LR);
		return artefactJsonParameterService.readAll(findMeasureEntityOrElseThrow(errandEntity, measureId).getJsonParameterLinks());
	}

	@Transactional(readOnly = true)
	public JsonParameter readMeasureJsonParameter(final String namespace, final String municipalityId, final String errandId, final String measureId, final String key) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, ProtectedResource.MEASURE, LR);
		return artefactJsonParameterService.read(findMeasureEntityOrElseThrow(errandEntity, measureId).getJsonParameterLinks(), key);
	}

	@Transactional
	public UpsertResult updateMeasureJsonParameter(final String namespace, final String municipalityId, final String errandId, final String measureId, final String key,
		final String ifMatch, final JsonParameter jsonParameter) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		final var entity = findMeasureEntityOrElseThrow(errandEntity, measureId);

		return artefactJsonParameterService.upsert(errandEntity, key, ifMatch, jsonParameter, jsonParameterLinks(entity),
			parameter -> MeasureJsonParameterEntity.create().withMeasureEntity(entity).withJsonParameterEntity(parameter), measureJsonParameterRepository);
	}

	@Transactional
	public void deleteMeasureJsonParameter(final String namespace, final String municipalityId, final String errandId, final String measureId, final String key, final String ifMatch) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		final var entity = findMeasureEntityOrElseThrow(errandEntity, measureId);

		artefactJsonParameterService.delete(errandEntity, jsonParameterLinks(entity), key, ifMatch);
	}

	/**
	 * Where the measure comes from, resolved through the errand rather than taken as an id and trusted. A decision or a
	 * statement belonging to another errand finds nothing and is answered as the 404 it is.
	 */
	private void applyProvenance(final String namespace, final String municipalityId, final String errandId, final Measure measure, final MeasureEntity entity) {
		ofNullable(measure.getDecisionId()).ifPresent(id -> entity.setDecisionEntity(
			decisionRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(namespace, municipalityId, errandId, id)
				.orElseThrow(() -> Problem.valueOf(NOT_FOUND, DECISION_NOT_FOUND.formatted(id, errandId)))));

		ofNullable(measure.getStatementId()).ifPresent(id -> entity.setStatementEntity(
			statementRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(namespace, municipalityId, errandId, id)
				.orElseThrow(() -> Problem.valueOf(NOT_FOUND, STATEMENT_NOT_FOUND.formatted(id, errandId)))));
	}

	private ArtefactLinks<MeasureAttachmentEntity> artefactLinks(final MeasureEntity entity) {
		return new ArtefactLinks<>(attachmentLinks(entity), attachment -> MeasureAttachmentEntity.create()
			.withMeasureEntity(entity)
			.withAttachmentEntity(attachment)
			.withCreatedBy(getCallerIdentity()), measureAttachmentRepository);
	}

	private List<MeasureAttachmentEntity> attachmentLinks(final MeasureEntity entity) {
		if (entity.getAttachments() == null) {
			entity.setAttachments(new ArrayList<>());
		}
		return entity.getAttachments();
	}

	private List<MeasureJsonParameterEntity> jsonParameterLinks(final MeasureEntity entity) {
		if (entity.getJsonParameterLinks() == null) {
			entity.setJsonParameterLinks(new ArrayList<>());
		}
		return entity.getJsonParameterLinks();
	}

	private MeasureEntity findMeasureEntityOrElseThrow(final ErrandEntity errandEntity, final String measureId) {
		return ofNullable(errandEntity.getMeasures()).orElse(emptyList()).stream()
			.filter(entity -> entity.getId().equals(measureId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, MEASURE_NOT_FOUND.formatted(measureId, errandEntity.getId())));
	}
}

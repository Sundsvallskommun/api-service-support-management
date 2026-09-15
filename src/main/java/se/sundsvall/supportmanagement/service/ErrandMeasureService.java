package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.integration.db.DecisionRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.StatementRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
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
import static se.sundsvall.supportmanagement.service.util.ETagUtil.validateIfMatch;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getCallerIdentity;

@Service
public class ErrandMeasureService {

	private static final String DECISION_NOT_FOUND = "A decision with id '%s' could not be found in errand with id '%s'";
	private static final String STATEMENT_NOT_FOUND = "A statement with id '%s' could not be found in errand with id '%s'";
	private static final String MEASURE_NOT_FOUND = "A measure with id '%s' could not be found in errand with id '%s'";

	private final ErrandsRepository errandsRepository;
	private final MeasureValidator measureValidator;
	private final DecisionRepository decisionRepository;
	private final StatementRepository statementRepository;
	private final ArtefactAttachmentService artefactAttachmentService;
	private final ArtefactJsonParameterService artefactJsonParameterService;
	private final AccessControlService accessControlService;
	private final EntityManager entityManager;

	ErrandMeasureService(final ErrandsRepository errandsRepository, final MeasureValidator measureValidator, final DecisionRepository decisionRepository,
		final StatementRepository statementRepository, final ArtefactAttachmentService artefactAttachmentService, final ArtefactJsonParameterService artefactJsonParameterService,
		final AccessControlService accessControlService, final EntityManager entityManager) {
		this.errandsRepository = errandsRepository;
		this.measureValidator = measureValidator;
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

	/**
	 * Flushed before the measure is mapped, so that the response carries the version just written - which is what its
	 * ETag is taken from.
	 */
	@Transactional
	public Measure updateErrandMeasure(final String namespace, final String municipalityId, final String errandId, final String measureId, final String ifMatch, final Measure measure) {
		measureValidator.validate(measure, namespace, municipalityId);

		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		entityManager.lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		final var measureEntity = findMeasureEntityOrElseThrow(errandEntity, measureId);
		validateIfMatch(ifMatch, measureEntity.getVersion());

		updateMeasureEntity(measureEntity, measure).setModifiedBy(getCallerIdentity());
		applyProvenance(namespace, municipalityId, errandId, measure, measureEntity);

		errandsRepository.saveAndFlush(errandEntity);
		return toMeasure(measureEntity);
	}

	@Transactional
	public void deleteErrandMeasure(final String namespace, final String municipalityId, final String errandId, final String measureId, final String ifMatch) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		entityManager.lock(errandEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		final var measureEntity = findMeasureEntityOrElseThrow(errandEntity, measureId);
		validateIfMatch(ifMatch, measureEntity.getVersion());

		ofNullable(errandEntity.getMeasures()).ifPresent(measures -> measures.remove(measureEntity));
		errandsRepository.save(errandEntity);
	}

	@Transactional
	public String createMeasureAttachment(final String namespace, final String municipalityId, final String errandId, final String measureId, final MultipartFile file) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		final var entity = findMeasureEntityOrElseThrow(errandEntity, measureId);

		final var attachmentId = artefactAttachmentService.uploadAndLink(namespace, municipalityId, errandId, file, attachments(entity));
		errandsRepository.saveAndFlush(errandEntity);
		return attachmentId;
	}

	@Transactional
	public ErrandAttachment linkMeasureAttachment(final String namespace, final String municipalityId, final String errandId, final String measureId, final String attachmentId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		final var entity = findMeasureEntityOrElseThrow(errandEntity, measureId);

		final var result = artefactAttachmentService.link(namespace, municipalityId, errandId, attachmentId, attachments(entity));
		errandsRepository.saveAndFlush(errandEntity);
		return result;
	}

	@Transactional
	public void unlinkMeasureAttachment(final String namespace, final String municipalityId, final String errandId, final String measureId, final String attachmentId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		final var entity = findMeasureEntityOrElseThrow(errandEntity, measureId);

		artefactAttachmentService.unlink(attachmentId, attachments(entity));
		errandsRepository.saveAndFlush(errandEntity);
	}

	@Transactional(readOnly = true)
	public List<JsonParameter> readMeasureJsonParameters(final String namespace, final String municipalityId, final String errandId, final String measureId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, ProtectedResource.MEASURE, LR);
		return artefactJsonParameterService.readAll(findMeasureEntityOrElseThrow(errandEntity, measureId).getJsonParameters());
	}

	@Transactional(readOnly = true)
	public JsonParameter readMeasureJsonParameter(final String namespace, final String municipalityId, final String errandId, final String measureId, final String key) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, ProtectedResource.MEASURE, LR);
		return artefactJsonParameterService.read(findMeasureEntityOrElseThrow(errandEntity, measureId).getJsonParameters(), key);
	}

	@Transactional
	public UpsertResult updateMeasureJsonParameter(final String namespace, final String municipalityId, final String errandId, final String measureId, final String ifMatch, final JsonParameter jsonParameter) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		final var entity = findMeasureEntityOrElseThrow(errandEntity, measureId);

		return artefactJsonParameterService.upsert(jsonParameters(entity), () -> MeasureJsonParameterEntity.create().withMeasureEntity(entity), ifMatch, jsonParameter);
	}

	@Transactional
	public void deleteMeasureJsonParameter(final String namespace, final String municipalityId, final String errandId, final String measureId, final String key, final String ifMatch) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.MEASURE, RW);
		final var entity = findMeasureEntityOrElseThrow(errandEntity, measureId);

		artefactJsonParameterService.delete(jsonParameters(entity), key, ifMatch);
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

	private List<AttachmentEntity> attachments(final MeasureEntity entity) {
		if (entity.getAttachments() == null) {
			entity.setAttachments(new ArrayList<>());
		}
		return entity.getAttachments();
	}

	private List<MeasureJsonParameterEntity> jsonParameters(final MeasureEntity entity) {
		if (entity.getJsonParameters() == null) {
			entity.setJsonParameters(new ArrayList<>());
		}
		return entity.getJsonParameters();
	}

	private MeasureEntity findMeasureEntityOrElseThrow(final ErrandEntity errandEntity, final String measureId) {
		return ofNullable(errandEntity.getMeasures()).orElse(emptyList()).stream()
			.filter(entity -> entity.getId().equals(measureId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, MEASURE_NOT_FOUND.formatted(measureId, errandEntity.getId())));
	}
}

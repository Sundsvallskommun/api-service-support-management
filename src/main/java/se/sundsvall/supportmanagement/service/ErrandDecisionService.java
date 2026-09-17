package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.errand.Decision;
import se.sundsvall.supportmanagement.api.model.errand.DecisionTerm;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.DecisionRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.InvestigationRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionTermEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static jakarta.persistence.LockModeType.OPTIMISTIC_FORCE_INCREMENT;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod.AUTOMATIC;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus.COMPLETED;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecision;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionTerm;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionTermEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionTerms;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisions;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.updateDecisionEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.updateDecisionTermEntity;
import static se.sundsvall.supportmanagement.service.util.ETagUtil.validateIfMatch;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getCallerIdentity;

/**
 * The decisions of an errand, and the terms they carry.
 * <p>
 * How many decisions an errand may hold is a setting of the namespace rather than a unique key, since interim
 * decisions, partial decisions and reconsideration are ordinary in some lines of business and unheard of in others.
 * {@link DecisionValidator} is what upholds it.
 * <p>
 * The investigation a decision rests on is resolved through the errand rather than taken as an id and trusted, which is
 * what keeps a decision from resting on the investigation of a different errand.
 * <p>
 * Creating, changing and deleting a decision is a change to the errand: it moves the version of the errand, so that a
 * work step holding an older one is told the basis it read has changed, and it writes an event with the sub type
 * DECISION, which is what wakes a process waiting for the decision. The terms, the attachment links and the JSON
 * parameters do neither. The process waits for the decision to be concluded, and every further event would only count
 * towards the emergency brake.
 * <p>
 * Every write but those to the JSON parameters is held to {@link DecisionValidator#validateChangeable}.
 */
@Service
public class ErrandDecisionService {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandDecisionService.class);
	private static final String DECISION_NOT_FOUND = "A decision with id '%s' could not be found in errand with id '%s'";
	private static final String TERM_NOT_FOUND = "A term with id '%s' could not be found in decision with id '%s'";
	private static final String INVESTIGATION_NOT_FOUND = "An investigation with id '%s' could not be found in errand with id '%s'";
	private static final String EVENT_LOG_CREATE_DECISION = "Ett beslut har lagts till i ärendet.";
	private static final String EVENT_LOG_UPDATE_DECISION = "Ett beslut i ärendet har uppdaterats.";
	private static final String EVENT_LOG_CONCLUDE_DECISION = "Ett beslut i ärendet har fattats.";
	private static final String EVENT_LOG_DELETE_DECISION = "Ett beslut har tagits bort från ärendet.";

	private final DecisionRepository decisionRepository;
	private final ArtefactAttachmentService artefactAttachmentService;
	private final ArtefactJsonParameterService artefactJsonParameterService;
	private final InvestigationRepository investigationRepository;
	private final ErrandProcessRepository processRepository;
	private final DecisionValidator decisionValidator;
	private final AccessControlService accessControlService;
	private final EventService eventService;
	private final EntityManager entityManager;

	ErrandDecisionService(final DecisionRepository decisionRepository, final ArtefactAttachmentService artefactAttachmentService, final ArtefactJsonParameterService artefactJsonParameterService,
		final InvestigationRepository investigationRepository, final ErrandProcessRepository processRepository, final DecisionValidator decisionValidator,
		final AccessControlService accessControlService, final EventService eventService, final EntityManager entityManager) {
		this.decisionRepository = decisionRepository;
		this.artefactAttachmentService = artefactAttachmentService;
		this.artefactJsonParameterService = artefactJsonParameterService;
		this.investigationRepository = investigationRepository;
		this.processRepository = processRepository;
		this.decisionValidator = decisionValidator;
		this.accessControlService = accessControlService;
		this.eventService = eventService;
		this.entityManager = entityManager;
	}

	@Transactional
	public String createErrandDecision(final String namespace, final String municipalityId, final String errandId, final Decision decision) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);
		final var method = ofNullable(decision.getMethod()).map(DecisionMethod::valueOf).orElse(null);

		decisionValidator.validateChangeable(errandId, null);
		decisionValidator.validateCardinality(namespace, municipalityId, errandId);
		decisionValidator.validateMethod(namespace, municipalityId, method);
		decisionValidator.validateOutcome(namespace, municipalityId, decision.getOutcome());

		final var investigationEntity = resolveInvestigation(namespace, municipalityId, errandId, decision.getInvestigationId());
		final var entity = toDecisionEntity(decision, errandEntity, investigationEntity, namespace, municipalityId)
			.withCreatedBy(getCallerIdentity())
			.withErrandProcessId(errandProcessIdOf(errandId, method, null));

		entityManager.lock(errandEntity, OPTIMISTIC_FORCE_INCREMENT);
		final var id = decisionRepository.saveAndFlush(entity).getId();

		final var concludes = COMPLETED == entity.getStatus();
		recordChange(errandEntity, concludes ? EVENT_LOG_CONCLUDE_DECISION : EVENT_LOG_CREATE_DECISION, concludes);
		return id;
	}

	@Transactional(readOnly = true)
	public Decision readErrandDecision(final String namespace, final String municipalityId, final String errandId, final String decisionId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.DECISION, LR);
		return toDecision(findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId));
	}

	@Transactional(readOnly = true)
	public List<Decision> findErrandDecisions(final String namespace, final String municipalityId, final String errandId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.DECISION, LR);
		return toDecisions(decisionRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(namespace, municipalityId, errandId));
	}

	/**
	 * A lock is answered before a stale version, since a decision that can no longer be changed will not become
	 * changeable by being read again.
	 */
	@Transactional
	public Decision updateErrandDecision(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String ifMatch, final Decision decision) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);

		final var entity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);
		decisionValidator.validateChangeable(errandId, entity);
		logMissingIfMatch(ifMatch, "PATCH", namespace, municipalityId, errandId, decisionId);
		validateIfMatch(ifMatch, entity.getVersion());

		final var method = ofNullable(decision.getMethod()).map(DecisionMethod::valueOf).orElse(entity.getMethod());
		decisionValidator.validateMethod(namespace, municipalityId, method);
		decisionValidator.validateOutcome(namespace, municipalityId, decision.getOutcome());

		final var wasCompleted = COMPLETED == entity.getStatus();
		updateDecisionEntity(entity, decision).setModifiedBy(getCallerIdentity());
		ofNullable(decision.getInvestigationId())
			.ifPresent(id -> entity.setInvestigationEntity(resolveInvestigation(namespace, municipalityId, errandId, id)));
		entity.setErrandProcessId(errandProcessIdOf(errandId, method, entity.getErrandProcessId()));

		entityManager.lock(errandEntity, OPTIMISTIC_FORCE_INCREMENT);
		final var result = toDecision(decisionRepository.saveAndFlush(entity));

		final var concludes = !wasCompleted && COMPLETED == entity.getStatus();
		recordChange(errandEntity, concludes ? EVENT_LOG_CONCLUDE_DECISION : EVENT_LOG_UPDATE_DECISION, concludes);
		return result;
	}

	@Transactional
	public void deleteErrandDecision(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String ifMatch) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);

		final var entity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);
		decisionValidator.validateChangeable(errandId, entity);
		logMissingIfMatch(ifMatch, "DELETE", namespace, municipalityId, errandId, decisionId);
		validateIfMatch(ifMatch, entity.getVersion());

		entityManager.lock(errandEntity, OPTIMISTIC_FORCE_INCREMENT);
		decisionRepository.delete(entity);
		decisionRepository.flush();

		recordChange(errandEntity, EVENT_LOG_DELETE_DECISION, false);
	}

	@Transactional
	public String createDecisionTerm(final String namespace, final String municipalityId, final String errandId, final String decisionId, final DecisionTerm term) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);

		final var decisionEntity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);
		decisionValidator.validateChangeable(errandId, decisionEntity);
		final var entity = toDecisionTermEntity(term, decisionEntity);
		if (decisionEntity.getTerms() == null) {
			decisionEntity.setTerms(new ArrayList<>());
		}
		decisionEntity.getTerms().add(entity);

		markChanged(decisionEntity);
		decisionRepository.flush();
		return entity.getId();
	}

	@Transactional(readOnly = true)
	public DecisionTerm readDecisionTerm(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String termId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.DECISION, LR);
		return toDecisionTerm(findTermOrElseThrow(findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId), termId));
	}

	@Transactional(readOnly = true)
	public List<DecisionTerm> findDecisionTerms(final String namespace, final String municipalityId, final String errandId, final String decisionId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.DECISION, LR);
		return toDecisionTerms(findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId).getTerms());
	}

	@Transactional
	public DecisionTerm updateDecisionTerm(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String termId, final DecisionTerm term) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);

		final var decisionEntity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);
		decisionValidator.validateChangeable(errandId, decisionEntity);
		final var entity = findTermOrElseThrow(decisionEntity, termId);
		updateDecisionTermEntity(entity, term);

		markChanged(decisionEntity);
		decisionRepository.saveAndFlush(decisionEntity);
		return toDecisionTerm(entity);
	}

	@Transactional
	public void deleteDecisionTerm(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String termId) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);

		final var decisionEntity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);
		decisionValidator.validateChangeable(errandId, decisionEntity);
		decisionEntity.getTerms().remove(findTermOrElseThrow(decisionEntity, termId));

		markChanged(decisionEntity);
		decisionRepository.saveAndFlush(decisionEntity);
	}

	@Transactional
	public String createDecisionAttachment(final String namespace, final String municipalityId, final String errandId, final String decisionId, final MultipartFile file) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);
		final var entity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);
		decisionValidator.validateChangeable(errandId, entity);

		final var attachmentId = artefactAttachmentService.uploadAndLink(namespace, municipalityId, errandId, file, attachments(entity));
		decisionRepository.saveAndFlush(entity);
		return attachmentId;
	}

	@Transactional
	public ErrandAttachment linkDecisionAttachment(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String attachmentId) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);
		final var entity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);
		decisionValidator.validateChangeable(errandId, entity);

		final var result = artefactAttachmentService.link(namespace, municipalityId, errandId, attachmentId, attachments(entity));
		decisionRepository.saveAndFlush(entity);
		return result;
	}

	@Transactional
	public void unlinkDecisionAttachment(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String attachmentId) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);
		final var entity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);
		decisionValidator.validateChangeable(errandId, entity);

		artefactAttachmentService.unlink(attachmentId, attachments(entity));
		decisionRepository.saveAndFlush(entity);
	}

	@Transactional(readOnly = true)
	public List<JsonParameter> readDecisionJsonParameters(final String namespace, final String municipalityId, final String errandId, final String decisionId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.DECISION, LR);
		return artefactJsonParameterService.readAll(findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId).getJsonParameters());
	}

	@Transactional(readOnly = true)
	public JsonParameter readDecisionJsonParameter(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String key) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.DECISION, LR);
		return artefactJsonParameterService.read(findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId).getJsonParameters(), key);
	}

	@Transactional
	public UpsertResult updateDecisionJsonParameter(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String ifMatch,
		final JsonParameter jsonParameter) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);
		final var entity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);

		return artefactJsonParameterService.upsert(jsonParameters(entity), () -> DecisionJsonParameterEntity.create().withDecisionEntity(entity), ifMatch, jsonParameter);
	}

	@Transactional
	public void deleteDecisionJsonParameter(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String key, final String ifMatch) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);
		final var entity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);

		artefactJsonParameterService.delete(jsonParameters(entity), key, ifMatch);
	}

	/**
	 * Writes the event of a change to the decision, which is how the process of the errand learns of it.
	 * <p>
	 * Caught and logged like every other errand event. A publication that fails has already marked the transaction for
	 * rollback, so the decision is not left saved while the process is never told.
	 */
	private void recordChange(final ErrandEntity errandEntity, final String message, final boolean concludesDecision) {
		try {
			eventService.createDecisionEvent(message, errandEntity, concludesDecision);
		} catch (final Exception e) {
			LOG.warn("Failed to log decision event for errand {}: {}", sanitizeForLogging(errandEntity.getId()), sanitizeForLogging(e.getMessage()));
		}
	}

	/**
	 * The process row a decision is made by. An automatic decision is made by the live process of the errand, and keeps
	 * the row it names when the errand has none live. A manual decision is made by no process.
	 */
	private String errandProcessIdOf(final String errandId, final DecisionMethod method, final String current) {
		if (AUTOMATIC != method) {
			return null;
		}
		return processRepository.findByErrandIdAndActiveMarkerIsNotNull(errandId)
			.map(ErrandProcessEntity::getId)
			.orElse(current);
	}

	private List<AttachmentEntity> attachments(final DecisionEntity entity) {
		if (entity.getAttachments() == null) {
			entity.setAttachments(new ArrayList<>());
		}
		return entity.getAttachments();
	}

	private List<DecisionJsonParameterEntity> jsonParameters(final DecisionEntity entity) {
		if (entity.getJsonParameters() == null) {
			entity.setJsonParameters(new ArrayList<>());
		}
		return entity.getJsonParameters();
	}

	private DecisionEntity findDecisionOrElseThrow(final String namespace, final String municipalityId, final String errandId, final String decisionId) {
		return decisionRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(namespace, municipalityId, errandId, decisionId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, DECISION_NOT_FOUND.formatted(decisionId, errandId)));
	}

	/**
	 * The terms are part of the decision as it is served, so a change to one of them moves the version its ETag carries -
	 * otherwise a caller holding the ETag from before would not be told the decision had changed.
	 */
	private void markChanged(final DecisionEntity decisionEntity) {
		entityManager.lock(decisionEntity, OPTIMISTIC_FORCE_INCREMENT);
	}

	private DecisionTermEntity findTermOrElseThrow(final DecisionEntity decisionEntity, final String termId) {
		return ofNullable(decisionEntity.getTerms()).orElse(emptyList()).stream()
			.filter(entity -> entity.getId().equals(termId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, TERM_NOT_FOUND.formatted(termId, decisionEntity.getId())));
	}

	/**
	 * The investigation is fetched through the errand, so an id belonging to another errand finds nothing and is
	 * answered as the 404 it is rather than written as a reference across errands.
	 */
	private InvestigationEntity resolveInvestigation(final String namespace, final String municipalityId, final String errandId, final String investigationId) {
		return ofNullable(investigationId)
			.map(id -> investigationRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(namespace, municipalityId, errandId, id)
				.orElseThrow(() -> Problem.valueOf(NOT_FOUND, INVESTIGATION_NOT_FOUND.formatted(id, errandId))))
			.orElse(null);
	}

	private void logMissingIfMatch(final String ifMatch, final String method, final String namespace, final String municipalityId, final String errandId, final String decisionId) {
		if (ifMatch == null) {
			LOG.debug("{} /errands/{}/decisions/{} received without If-Match header (namespace={}, municipalityId={})", method, sanitizeForLogging(errandId), sanitizeForLogging(decisionId),
				sanitizeForLogging(namespace), sanitizeForLogging(municipalityId));
		}
	}
}

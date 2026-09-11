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
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachment;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachmentLink;
import se.sundsvall.supportmanagement.api.model.errand.Decision;
import se.sundsvall.supportmanagement.api.model.errand.DecisionTerm;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.DecisionAttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.DecisionJsonParameterRepository;
import se.sundsvall.supportmanagement.integration.db.DecisionRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.InvestigationRepository;
import se.sundsvall.supportmanagement.integration.db.model.DecisionAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionTermEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.ArtefactAttachmentService.ArtefactLinks;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static jakarta.persistence.LockModeType.OPTIMISTIC_FORCE_INCREMENT;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecision;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionTerm;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionTermEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisionTerms;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.toDecisions;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.updateDecisionEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandDecisionMapper.updateDecisionTermEntity;
import static se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters.ownedParameterIds;
import static se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters.removeParameters;
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
 */
@Service
public class ErrandDecisionService {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandDecisionService.class);
	private static final String DECISION_NOT_FOUND = "A decision with id '%s' could not be found in errand with id '%s'";
	private static final String TERM_NOT_FOUND = "A term with id '%s' could not be found in decision with id '%s'";
	private static final String INVESTIGATION_NOT_FOUND = "An investigation with id '%s' could not be found in errand with id '%s'";

	private final ErrandsRepository errandsRepository;
	private final DecisionRepository decisionRepository;
	private final DecisionAttachmentRepository decisionAttachmentRepository;
	private final DecisionJsonParameterRepository decisionJsonParameterRepository;
	private final ArtefactAttachmentService artefactAttachmentService;
	private final ArtefactJsonParameterService artefactJsonParameterService;
	private final InvestigationRepository investigationRepository;
	private final DecisionValidator decisionValidator;
	private final AccessControlService accessControlService;
	private final EntityManager entityManager;

	ErrandDecisionService(final ErrandsRepository errandsRepository, final DecisionRepository decisionRepository, final DecisionAttachmentRepository decisionAttachmentRepository,
		final DecisionJsonParameterRepository decisionJsonParameterRepository, final ArtefactAttachmentService artefactAttachmentService,
		final ArtefactJsonParameterService artefactJsonParameterService, final InvestigationRepository investigationRepository,
		final DecisionValidator decisionValidator, final AccessControlService accessControlService, final EntityManager entityManager) {
		this.errandsRepository = errandsRepository;
		this.decisionRepository = decisionRepository;
		this.decisionAttachmentRepository = decisionAttachmentRepository;
		this.decisionJsonParameterRepository = decisionJsonParameterRepository;
		this.artefactAttachmentService = artefactAttachmentService;
		this.artefactJsonParameterService = artefactJsonParameterService;
		this.investigationRepository = investigationRepository;
		this.decisionValidator = decisionValidator;
		this.accessControlService = accessControlService;
		this.entityManager = entityManager;
	}

	@Transactional
	public String createErrandDecision(final String namespace, final String municipalityId, final String errandId, final Decision decision) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);

		decisionValidator.validateCardinality(namespace, municipalityId, errandId);
		decisionValidator.validateMethod(ofNullable(decision.getMethod()).map(DecisionMethod::valueOf).orElse(null));

		final var investigationEntity = resolveInvestigation(namespace, municipalityId, errandId, decision.getInvestigationId());
		final var entity = toDecisionEntity(decision, errandEntity, investigationEntity, namespace, municipalityId)
			.withCreatedBy(getCallerIdentity());

		return decisionRepository.save(entity).getId();
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

	@Transactional
	public Decision updateErrandDecision(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String ifMatch, final Decision decision) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);

		final var entity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);
		logMissingIfMatch(ifMatch, "PATCH", namespace, municipalityId, errandId, decisionId);
		validateIfMatch(ifMatch, entity.getVersion());

		decisionValidator.validateMethod(ofNullable(decision.getMethod()).map(DecisionMethod::valueOf).orElse(entity.getMethod()));

		updateDecisionEntity(entity, decision).setModifiedBy(getCallerIdentity());
		ofNullable(decision.getInvestigationId())
			.ifPresent(id -> entity.setInvestigationEntity(resolveInvestigation(namespace, municipalityId, errandId, id)));

		return toDecision(decisionRepository.saveAndFlush(entity));
	}

	@Transactional
	public void deleteErrandDecision(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String ifMatch) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);

		final var entity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);
		logMissingIfMatch(ifMatch, "DELETE", namespace, municipalityId, errandId, decisionId);
		validateIfMatch(ifMatch, entity.getVersion());

		// Named now: the links that name them go with the decision.
		final var ownedParameters = ownedParameterIds(entity.getJsonParameterLinks());

		decisionRepository.delete(entity);
		decisionRepository.flush();

		removeParameters(errandEntity, ownedParameters);
		errandsRepository.saveAndFlush(errandEntity);
	}

	@Transactional
	public String createDecisionTerm(final String namespace, final String municipalityId, final String errandId, final String decisionId, final DecisionTerm term) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);

		final var decisionEntity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);
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
		decisionEntity.getTerms().remove(findTermOrElseThrow(decisionEntity, termId));

		markChanged(decisionEntity);
		decisionRepository.saveAndFlush(decisionEntity);
	}

	@Transactional
	public String createDecisionAttachment(final String namespace, final String municipalityId, final String errandId, final String decisionId, final MultipartFile file, final Integer sortOrder) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);
		final var entity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);

		return artefactAttachmentService.uploadAndLink(namespace, municipalityId, errandId, file, sortOrder, artefactLinks(entity));
	}

	@Transactional
	public ArtefactAttachment linkDecisionAttachment(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String attachmentId,
		final ArtefactAttachmentLink link) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);
		final var entity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);

		return artefactAttachmentService.link(namespace, municipalityId, errandId, attachmentId, link.getSortOrder(), artefactLinks(entity));
	}

	@Transactional
	public ArtefactAttachment updateDecisionAttachment(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String attachmentId,
		final ArtefactAttachmentLink link) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);
		final var entity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);

		return artefactAttachmentService.update(attachmentId, link, attachmentLinks(entity));
	}

	@Transactional
	public void unlinkDecisionAttachment(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String attachmentId) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);
		final var entity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);

		artefactAttachmentService.unlink(attachmentId, attachmentLinks(entity));
		decisionRepository.saveAndFlush(entity);
	}

	@Transactional(readOnly = true)
	public List<JsonParameter> readDecisionJsonParameters(final String namespace, final String municipalityId, final String errandId, final String decisionId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, ProtectedResource.DECISION, LR);
		return artefactJsonParameterService.readAll(errandEntity, findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId).getJsonParameterLinks());
	}

	@Transactional(readOnly = true)
	public JsonParameter readDecisionJsonParameter(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String key) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, ProtectedResource.DECISION, LR);
		return artefactJsonParameterService.read(errandEntity, findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId).getJsonParameterLinks(), key);
	}

	@Transactional
	public UpsertResult updateDecisionJsonParameter(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String ifMatch,
		final JsonParameter jsonParameter) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);
		final var entity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);

		return artefactJsonParameterService.upsert(errandEntity, ifMatch, jsonParameter, jsonParameterLinks(entity),
			parameter -> DecisionJsonParameterEntity.create().withDecisionEntity(entity).withJsonParameterEntity(parameter), decisionJsonParameterRepository);
	}

	@Transactional
	public void deleteDecisionJsonParameter(final String namespace, final String municipalityId, final String errandId, final String decisionId, final String key, final String ifMatch) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.DECISION, RW);
		final var entity = findDecisionOrElseThrow(namespace, municipalityId, errandId, decisionId);

		artefactJsonParameterService.delete(errandEntity, jsonParameterLinks(entity), key, ifMatch);
	}

	private ArtefactLinks<DecisionAttachmentEntity> artefactLinks(final DecisionEntity entity) {
		return new ArtefactLinks<>(attachmentLinks(entity), attachment -> DecisionAttachmentEntity.create()
			.withDecisionEntity(entity)
			.withAttachmentEntity(attachment)
			.withCreatedBy(getCallerIdentity()), decisionAttachmentRepository);
	}

	private List<DecisionAttachmentEntity> attachmentLinks(final DecisionEntity entity) {
		if (entity.getAttachments() == null) {
			entity.setAttachments(new ArrayList<>());
		}
		return entity.getAttachments();
	}

	private List<DecisionJsonParameterEntity> jsonParameterLinks(final DecisionEntity entity) {
		if (entity.getJsonParameterLinks() == null) {
			entity.setJsonParameterLinks(new ArrayList<>());
		}
		return entity.getJsonParameterLinks();
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

package se.sundsvall.supportmanagement.service;

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
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.model.errand.Statement;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.StatementAttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.StatementJsonParameterRepository;
import se.sundsvall.supportmanagement.integration.db.StatementRepository;
import se.sundsvall.supportmanagement.integration.db.model.StatementAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.ArtefactAttachmentService.ArtefactLinks;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.service.mapper.ErrandStatementMapper.toStatement;
import static se.sundsvall.supportmanagement.service.mapper.ErrandStatementMapper.toStatementEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandStatementMapper.toStatements;
import static se.sundsvall.supportmanagement.service.mapper.ErrandStatementMapper.updateStatementEntity;
import static se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters.ownedParameterIds;
import static se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters.removeParameters;
import static se.sundsvall.supportmanagement.service.util.ETagUtil.validateIfMatch;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getCallerIdentity;

/**
 * The statements of an errand.
 * <p>
 * A statement is reached through its errand and never past it: the errand is fetched and authorized first, and the
 * statement is then looked up by namespace, municipality, errand and id together. Belonging to the errand is therefore
 * a consequence of the lookup rather than a check that can be forgotten, and asking for a statement of another errand
 * is a 404 rather than a leak.
 * <p>
 * The version of the errand is deliberately left alone. A statement is a resource of its own with a version of its own,
 * and answering a statement should not make every held ETag for the errand stale.
 */
@Service
public class ErrandStatementService {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandStatementService.class);
	private static final String STATEMENT_NOT_FOUND = "A statement with id '%s' could not be found in errand with id '%s'";

	private final ErrandsRepository errandsRepository;
	private final StatementRepository statementRepository;
	private final StatementAttachmentRepository statementAttachmentRepository;
	private final StatementJsonParameterRepository statementJsonParameterRepository;
	private final StatementValidator statementValidator;
	private final ArtefactAttachmentService artefactAttachmentService;
	private final ArtefactJsonParameterService artefactJsonParameterService;
	private final AccessControlService accessControlService;

	ErrandStatementService(final ErrandsRepository errandsRepository, final StatementRepository statementRepository, final StatementAttachmentRepository statementAttachmentRepository,
		final StatementJsonParameterRepository statementJsonParameterRepository, final StatementValidator statementValidator, final ArtefactAttachmentService artefactAttachmentService,
		final ArtefactJsonParameterService artefactJsonParameterService, final AccessControlService accessControlService) {
		this.errandsRepository = errandsRepository;
		this.statementRepository = statementRepository;
		this.statementAttachmentRepository = statementAttachmentRepository;
		this.statementJsonParameterRepository = statementJsonParameterRepository;
		this.statementValidator = statementValidator;
		this.artefactAttachmentService = artefactAttachmentService;
		this.artefactJsonParameterService = artefactJsonParameterService;
		this.accessControlService = accessControlService;
	}

	@Transactional
	public String createErrandStatement(final String namespace, final String municipalityId, final String errandId, final Statement statement) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.STATEMENT, RW);

		final var entity = toStatementEntity(statement, errandEntity, namespace, municipalityId)
			.withCreatedBy(getCallerIdentity());
		statementValidator.validate(entity);

		return statementRepository.save(entity).getId();
	}

	@Transactional(readOnly = true)
	public Statement readErrandStatement(final String namespace, final String municipalityId, final String errandId, final String statementId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.STATEMENT, LR);
		return toStatement(findStatementOrElseThrow(namespace, municipalityId, errandId, statementId));
	}

	@Transactional(readOnly = true)
	public List<Statement> findErrandStatements(final String namespace, final String municipalityId, final String errandId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.STATEMENT, LR);
		return toStatements(statementRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(namespace, municipalityId, errandId));
	}

	@Transactional
	public Statement updateErrandStatement(final String namespace, final String municipalityId, final String errandId, final String statementId, final String ifMatch, final Statement statement) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.STATEMENT, RW);

		final var entity = findStatementOrElseThrow(namespace, municipalityId, errandId, statementId);
		logMissingIfMatch(ifMatch, "PATCH", namespace, municipalityId, errandId, statementId);
		validateIfMatch(ifMatch, entity.getVersion());

		updateStatementEntity(entity, statement).setModifiedBy(getCallerIdentity());
		statementValidator.validate(entity);

		return toStatement(statementRepository.saveAndFlush(entity));
	}

	@Transactional
	public void deleteErrandStatement(final String namespace, final String municipalityId, final String errandId, final String statementId, final String ifMatch) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.STATEMENT, RW);

		final var entity = findStatementOrElseThrow(namespace, municipalityId, errandId, statementId);
		logMissingIfMatch(ifMatch, "DELETE", namespace, municipalityId, errandId, statementId);
		validateIfMatch(ifMatch, entity.getVersion());

		// Named now: the links that name them go with the statement.
		final var ownedParameters = ownedParameterIds(entity.getJsonParameterLinks());

		statementRepository.delete(entity);
		statementRepository.flush();

		removeParameters(errandEntity, ownedParameters);
		errandsRepository.saveAndFlush(errandEntity);
	}

	@Transactional
	public String createStatementAttachment(final String namespace, final String municipalityId, final String errandId, final String statementId, final MultipartFile file, final Integer sortOrder) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.STATEMENT, RW);
		final var entity = findStatementOrElseThrow(namespace, municipalityId, errandId, statementId);

		return artefactAttachmentService.uploadAndLink(namespace, municipalityId, errandId, file, sortOrder, artefactLinks(entity));
	}

	@Transactional
	public ArtefactAttachment linkStatementAttachment(final String namespace, final String municipalityId, final String errandId, final String statementId, final String attachmentId,
		final ArtefactAttachmentLink link) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.STATEMENT, RW);
		final var entity = findStatementOrElseThrow(namespace, municipalityId, errandId, statementId);

		return artefactAttachmentService.link(namespace, municipalityId, errandId, attachmentId, link.getSortOrder(), artefactLinks(entity));
	}

	@Transactional
	public ArtefactAttachment updateStatementAttachment(final String namespace, final String municipalityId, final String errandId, final String statementId, final String attachmentId,
		final ArtefactAttachmentLink link) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.STATEMENT, RW);
		final var entity = findStatementOrElseThrow(namespace, municipalityId, errandId, statementId);

		return artefactAttachmentService.update(attachmentId, link, attachmentLinks(entity));
	}

	@Transactional
	public void unlinkStatementAttachment(final String namespace, final String municipalityId, final String errandId, final String statementId, final String attachmentId) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.STATEMENT, RW);
		final var entity = findStatementOrElseThrow(namespace, municipalityId, errandId, statementId);

		artefactAttachmentService.unlink(attachmentId, attachmentLinks(entity));
		statementRepository.saveAndFlush(entity);
	}

	@Transactional(readOnly = true)
	public List<JsonParameter> readStatementJsonParameters(final String namespace, final String municipalityId, final String errandId, final String statementId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, ProtectedResource.STATEMENT, LR);
		return artefactJsonParameterService.readAll(errandEntity, findStatementOrElseThrow(namespace, municipalityId, errandId, statementId).getJsonParameterLinks());
	}

	@Transactional(readOnly = true)
	public JsonParameter readStatementJsonParameter(final String namespace, final String municipalityId, final String errandId, final String statementId, final String key) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, ProtectedResource.STATEMENT, LR);
		return artefactJsonParameterService.read(errandEntity, findStatementOrElseThrow(namespace, municipalityId, errandId, statementId).getJsonParameterLinks(), key);
	}

	@Transactional
	public UpsertResult updateStatementJsonParameter(final String namespace, final String municipalityId, final String errandId, final String statementId, final String ifMatch,
		final JsonParameter jsonParameter) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.STATEMENT, RW);
		final var entity = findStatementOrElseThrow(namespace, municipalityId, errandId, statementId);

		return artefactJsonParameterService.upsert(errandEntity, ifMatch, jsonParameter, jsonParameterLinks(entity),
			parameter -> StatementJsonParameterEntity.create().withStatementEntity(entity).withJsonParameterEntity(parameter), statementJsonParameterRepository);
	}

	@Transactional
	public void deleteStatementJsonParameter(final String namespace, final String municipalityId, final String errandId, final String statementId, final String key, final String ifMatch) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.STATEMENT, RW);
		final var entity = findStatementOrElseThrow(namespace, municipalityId, errandId, statementId);

		artefactJsonParameterService.delete(errandEntity, jsonParameterLinks(entity), key, ifMatch);
	}

	private ArtefactLinks<StatementAttachmentEntity> artefactLinks(final StatementEntity entity) {
		return new ArtefactLinks<>(attachmentLinks(entity), attachment -> StatementAttachmentEntity.create()
			.withStatementEntity(entity)
			.withAttachmentEntity(attachment)
			.withCreatedBy(getCallerIdentity()), statementAttachmentRepository);
	}

	private List<StatementAttachmentEntity> attachmentLinks(final StatementEntity entity) {
		if (entity.getAttachments() == null) {
			entity.setAttachments(new ArrayList<>());
		}
		return entity.getAttachments();
	}

	private List<StatementJsonParameterEntity> jsonParameterLinks(final StatementEntity entity) {
		if (entity.getJsonParameterLinks() == null) {
			entity.setJsonParameterLinks(new ArrayList<>());
		}
		return entity.getJsonParameterLinks();
	}

	private StatementEntity findStatementOrElseThrow(final String namespace, final String municipalityId, final String errandId, final String statementId) {
		return statementRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(namespace, municipalityId, errandId, statementId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, STATEMENT_NOT_FOUND.formatted(statementId, errandId)));
	}

	private void logMissingIfMatch(final String ifMatch, final String method, final String namespace, final String municipalityId, final String errandId, final String statementId) {
		if (ifMatch == null) {
			LOG.debug("{} /errands/{}/statements/{} received without If-Match header (namespace={}, municipalityId={})", method, sanitizeForLogging(errandId), sanitizeForLogging(statementId),
				sanitizeForLogging(namespace), sanitizeForLogging(municipalityId));
		}
	}
}

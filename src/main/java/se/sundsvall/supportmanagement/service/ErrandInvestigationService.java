package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachment;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachmentLink;
import se.sundsvall.supportmanagement.api.model.errand.Investigation;
import se.sundsvall.supportmanagement.api.model.errand.InvestigationSection;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.InvestigationAttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.InvestigationJsonParameterRepository;
import se.sundsvall.supportmanagement.integration.db.InvestigationRepository;
import se.sundsvall.supportmanagement.integration.db.InvestigationSectionJsonParameterRepository;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationSectionEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationSectionJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.ArtefactAttachmentService.ArtefactLinks;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static jakarta.persistence.LockModeType.OPTIMISTIC_FORCE_INCREMENT;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigation;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigationEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigationSection;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigationSectionEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigationSections;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.toInvestigations;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.updateInvestigationEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandInvestigationMapper.updateInvestigationSectionEntity;
import static se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters.ownedParameterIds;
import static se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters.removeParameters;
import static se.sundsvall.supportmanagement.service.util.ETagUtil.validateIfMatch;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getCallerIdentity;

/**
 * The investigations of an errand, and the sections they are assessed in.
 * <p>
 * The sections are reached through their investigation the same way the investigation is reached through its errand,
 * and are cascaded by it - they have no life outside the investigation they belong to.
 */
@Service
public class ErrandInvestigationService {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandInvestigationService.class);
	private static final String INVESTIGATION_NOT_FOUND = "An investigation with id '%s' could not be found in errand with id '%s'";
	private static final String SECTION_NOT_FOUND = "A section with id '%s' could not be found in investigation with id '%s'";
	private static final String SECTION_KEY_TAKEN = "A section with key '%s' already exists in investigation with id '%s'";

	private final ErrandsRepository errandsRepository;
	private final InvestigationRepository investigationRepository;
	private final InvestigationAttachmentRepository investigationAttachmentRepository;
	private final InvestigationJsonParameterRepository investigationJsonParameterRepository;
	private final InvestigationSectionJsonParameterRepository investigationSectionJsonParameterRepository;
	private final ArtefactAttachmentService artefactAttachmentService;
	private final ArtefactJsonParameterService artefactJsonParameterService;
	private final AccessControlService accessControlService;
	private final EntityManager entityManager;

	ErrandInvestigationService(final ErrandsRepository errandsRepository, final InvestigationRepository investigationRepository,
		final InvestigationAttachmentRepository investigationAttachmentRepository, final InvestigationJsonParameterRepository investigationJsonParameterRepository,
		final InvestigationSectionJsonParameterRepository investigationSectionJsonParameterRepository, final ArtefactAttachmentService artefactAttachmentService,
		final ArtefactJsonParameterService artefactJsonParameterService, final AccessControlService accessControlService, final EntityManager entityManager) {
		this.errandsRepository = errandsRepository;
		this.investigationRepository = investigationRepository;
		this.investigationAttachmentRepository = investigationAttachmentRepository;
		this.investigationJsonParameterRepository = investigationJsonParameterRepository;
		this.investigationSectionJsonParameterRepository = investigationSectionJsonParameterRepository;
		this.artefactAttachmentService = artefactAttachmentService;
		this.artefactJsonParameterService = artefactJsonParameterService;
		this.accessControlService = accessControlService;
		this.entityManager = entityManager;
	}

	@Transactional
	public String createErrandInvestigation(final String namespace, final String municipalityId, final String errandId, final Investigation investigation) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.INVESTIGATION, RW);

		final var entity = toInvestigationEntity(investigation, errandEntity, namespace, municipalityId)
			.withCreatedBy(getCallerIdentity());

		return investigationRepository.save(entity).getId();
	}

	@Transactional(readOnly = true)
	public Investigation readErrandInvestigation(final String namespace, final String municipalityId, final String errandId, final String investigationId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.INVESTIGATION, LR);
		return toInvestigation(findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId));
	}

	@Transactional(readOnly = true)
	public List<Investigation> findErrandInvestigations(final String namespace, final String municipalityId, final String errandId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.INVESTIGATION, LR);
		return toInvestigations(investigationRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdOrderByCreated(namespace, municipalityId, errandId));
	}

	@Transactional
	public Investigation updateErrandInvestigation(final String namespace, final String municipalityId, final String errandId, final String investigationId, final String ifMatch,
		final Investigation investigation) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.INVESTIGATION, RW);

		final var entity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);
		logMissingIfMatch(ifMatch, "PATCH", namespace, municipalityId, errandId, investigationId);
		validateIfMatch(ifMatch, entity.getVersion());

		updateInvestigationEntity(entity, investigation).setModifiedBy(getCallerIdentity());

		return toInvestigation(investigationRepository.saveAndFlush(entity));
	}

	@Transactional
	public void deleteErrandInvestigation(final String namespace, final String municipalityId, final String errandId, final String investigationId, final String ifMatch) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.INVESTIGATION, RW);

		final var entity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);
		logMissingIfMatch(ifMatch, "DELETE", namespace, municipalityId, errandId, investigationId);
		validateIfMatch(ifMatch, entity.getVersion());

		// Named now: the links that name them go with the investigation, and so do those of its sections.
		final var ownedParameters = new HashSet<>(ownedParameterIds(entity.getJsonParameterLinks()));
		ofNullable(entity.getSections()).orElse(emptyList())
			.forEach(section -> ownedParameters.addAll(ownedParameterIds(section.getJsonParameterLinks())));

		investigationRepository.delete(entity);
		investigationRepository.flush();

		removeParameters(errandEntity, ownedParameters);
		errandsRepository.saveAndFlush(errandEntity);
	}

	@Transactional
	public String createInvestigationSection(final String namespace, final String municipalityId, final String errandId, final String investigationId, final InvestigationSection section) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.INVESTIGATION, RW);

		final var investigationEntity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);
		verifySectionKeyIsFree(investigationEntity, section.getSectionKey(), null);

		final var entity = toInvestigationSectionEntity(section, investigationEntity);
		if (investigationEntity.getSections() == null) {
			investigationEntity.setSections(new ArrayList<>());
		}
		investigationEntity.getSections().add(entity);

		markChanged(investigationEntity);
		investigationRepository.flush();
		return entity.getId();
	}

	@Transactional(readOnly = true)
	public InvestigationSection readInvestigationSection(final String namespace, final String municipalityId, final String errandId, final String investigationId, final String sectionId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.INVESTIGATION, LR);
		final var investigationEntity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);
		return toInvestigationSection(findSectionOrElseThrow(investigationEntity, sectionId));
	}

	@Transactional(readOnly = true)
	public List<InvestigationSection> findInvestigationSections(final String namespace, final String municipalityId, final String errandId, final String investigationId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.INVESTIGATION, LR);
		return toInvestigationSections(findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId).getSections());
	}

	@Transactional
	public InvestigationSection updateInvestigationSection(final String namespace, final String municipalityId, final String errandId, final String investigationId, final String sectionId,
		final InvestigationSection section) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.INVESTIGATION, RW);

		final var investigationEntity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);
		final var entity = findSectionOrElseThrow(investigationEntity, sectionId);
		verifySectionKeyIsFree(investigationEntity, section.getSectionKey(), sectionId);

		updateInvestigationSectionEntity(entity, section);

		markChanged(investigationEntity);
		investigationRepository.saveAndFlush(investigationEntity);
		return toInvestigationSection(entity);
	}

	@Transactional
	public void deleteInvestigationSection(final String namespace, final String municipalityId, final String errandId, final String investigationId, final String sectionId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.INVESTIGATION, RW);

		final var investigationEntity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);
		final var entity = findSectionOrElseThrow(investigationEntity, sectionId);

		final var ownedParameters = ownedParameterIds(entity.getJsonParameterLinks());

		investigationEntity.getSections().remove(entity);
		markChanged(investigationEntity);
		investigationRepository.saveAndFlush(investigationEntity);

		removeParameters(errandEntity, ownedParameters);
		errandsRepository.saveAndFlush(errandEntity);
	}

	@Transactional
	public String createInvestigationAttachment(final String namespace, final String municipalityId, final String errandId, final String investigationId, final MultipartFile file, final Integer sortOrder) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.INVESTIGATION, RW);
		final var entity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);

		return artefactAttachmentService.uploadAndLink(namespace, municipalityId, errandId, file, sortOrder, artefactLinks(entity));
	}

	@Transactional
	public ArtefactAttachment linkInvestigationAttachment(final String namespace, final String municipalityId, final String errandId, final String investigationId, final String attachmentId,
		final ArtefactAttachmentLink link) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.INVESTIGATION, RW);
		final var entity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);

		return artefactAttachmentService.link(namespace, municipalityId, errandId, attachmentId, link.getSortOrder(), artefactLinks(entity));
	}

	@Transactional
	public ArtefactAttachment updateInvestigationAttachment(final String namespace, final String municipalityId, final String errandId, final String investigationId, final String attachmentId,
		final ArtefactAttachmentLink link) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.INVESTIGATION, RW);
		final var entity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);

		return artefactAttachmentService.update(attachmentId, link, attachmentLinks(entity));
	}

	@Transactional
	public void unlinkInvestigationAttachment(final String namespace, final String municipalityId, final String errandId, final String investigationId, final String attachmentId) {
		accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.INVESTIGATION, RW);
		final var entity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);

		artefactAttachmentService.unlink(attachmentId, attachmentLinks(entity));
		investigationRepository.saveAndFlush(entity);
	}

	@Transactional(readOnly = true)
	public List<JsonParameter> readInvestigationJsonParameters(final String namespace, final String municipalityId, final String errandId, final String investigationId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.INVESTIGATION, LR);
		return artefactJsonParameterService.readAll(findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId).getJsonParameterLinks());
	}

	@Transactional(readOnly = true)
	public JsonParameter readInvestigationJsonParameter(final String namespace, final String municipalityId, final String errandId, final String investigationId, final String key) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.INVESTIGATION, LR);
		return artefactJsonParameterService.read(findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId).getJsonParameterLinks(), key);
	}

	@Transactional
	public UpsertResult updateInvestigationJsonParameter(final String namespace, final String municipalityId, final String errandId, final String investigationId, final String key,
		final String ifMatch, final JsonParameter jsonParameter) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.INVESTIGATION, RW);
		final var entity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);

		return artefactJsonParameterService.upsert(errandEntity, key, ifMatch, jsonParameter, jsonParameterLinks(entity),
			parameter -> InvestigationJsonParameterEntity.create().withInvestigationEntity(entity).withJsonParameterEntity(parameter), investigationJsonParameterRepository);
	}

	@Transactional
	public void deleteInvestigationJsonParameter(final String namespace, final String municipalityId, final String errandId, final String investigationId, final String key, final String ifMatch) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.INVESTIGATION, RW);
		final var entity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);

		artefactJsonParameterService.delete(errandEntity, jsonParameterLinks(entity), key, ifMatch);
	}

	@Transactional(readOnly = true)
	public List<JsonParameter> readSectionJsonParameters(final String namespace, final String municipalityId, final String errandId, final String investigationId, final String sectionId) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.INVESTIGATION, LR);
		final var investigationEntity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);
		return artefactJsonParameterService.readAll(findSectionOrElseThrow(investigationEntity, sectionId).getJsonParameterLinks());
	}

	@Transactional(readOnly = true)
	public JsonParameter readSectionJsonParameter(final String namespace, final String municipalityId, final String errandId, final String investigationId, final String sectionId, final String key) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, ProtectedResource.INVESTIGATION, LR);
		final var investigationEntity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);
		return artefactJsonParameterService.read(findSectionOrElseThrow(investigationEntity, sectionId).getJsonParameterLinks(), key);
	}

	@Transactional
	public UpsertResult updateSectionJsonParameter(final String namespace, final String municipalityId, final String errandId, final String investigationId, final String sectionId, final String key,
		final String ifMatch, final JsonParameter jsonParameter) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.INVESTIGATION, RW);
		final var investigationEntity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);
		final var entity = findSectionOrElseThrow(investigationEntity, sectionId);

		return artefactJsonParameterService.upsert(errandEntity, key, ifMatch, jsonParameter, sectionJsonParameterLinks(entity),
			parameter -> InvestigationSectionJsonParameterEntity.create().withInvestigationSectionEntity(entity).withJsonParameterEntity(parameter), investigationSectionJsonParameterRepository);
	}

	@Transactional
	public void deleteSectionJsonParameter(final String namespace, final String municipalityId, final String errandId, final String investigationId, final String sectionId, final String key, final String ifMatch) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, true, ProtectedResource.INVESTIGATION, RW);
		final var investigationEntity = findInvestigationOrElseThrow(namespace, municipalityId, errandId, investigationId);

		artefactJsonParameterService.delete(errandEntity, sectionJsonParameterLinks(findSectionOrElseThrow(investigationEntity, sectionId)), key, ifMatch);
	}

	private ArtefactLinks<InvestigationAttachmentEntity> artefactLinks(final InvestigationEntity entity) {
		return new ArtefactLinks<>(attachmentLinks(entity), attachment -> InvestigationAttachmentEntity.create()
			.withInvestigationEntity(entity)
			.withAttachmentEntity(attachment)
			.withCreatedBy(getCallerIdentity()), investigationAttachmentRepository);
	}

	private List<InvestigationAttachmentEntity> attachmentLinks(final InvestigationEntity entity) {
		if (entity.getAttachments() == null) {
			entity.setAttachments(new ArrayList<>());
		}
		return entity.getAttachments();
	}

	private List<InvestigationJsonParameterEntity> jsonParameterLinks(final InvestigationEntity entity) {
		if (entity.getJsonParameterLinks() == null) {
			entity.setJsonParameterLinks(new ArrayList<>());
		}
		return entity.getJsonParameterLinks();
	}

	private List<InvestigationSectionJsonParameterEntity> sectionJsonParameterLinks(final InvestigationSectionEntity entity) {
		if (entity.getJsonParameterLinks() == null) {
			entity.setJsonParameterLinks(new ArrayList<>());
		}
		return entity.getJsonParameterLinks();
	}

	private InvestigationEntity findInvestigationOrElseThrow(final String namespace, final String municipalityId, final String errandId, final String investigationId) {
		return investigationRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(namespace, municipalityId, errandId, investigationId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, INVESTIGATION_NOT_FOUND.formatted(investigationId, errandId)));
	}

	/**
	 * The sections are part of the investigation as it is served, so a change to one of them moves the version its ETag
	 * carries - otherwise a caller holding the ETag from before would not be told the investigation had changed.
	 */
	private void markChanged(final InvestigationEntity investigationEntity) {
		entityManager.lock(investigationEntity, OPTIMISTIC_FORCE_INCREMENT);
	}

	private InvestigationSectionEntity findSectionOrElseThrow(final InvestigationEntity investigationEntity, final String sectionId) {
		return ofNullable(investigationEntity.getSections()).orElse(emptyList()).stream()
			.filter(entity -> entity.getId().equals(sectionId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, SECTION_NOT_FOUND.formatted(sectionId, investigationEntity.getId())));
	}

	/**
	 * The section key is unique per investigation in the database. Checking it here turns what would surface as a
	 * constraint violation deep in the flush into the conflict it is.
	 */
	private void verifySectionKeyIsFree(final InvestigationEntity investigationEntity, final String sectionKey, final String ownSectionId) {
		ofNullable(sectionKey)
			.filter(key -> ofNullable(investigationEntity.getSections()).orElse(emptyList()).stream()
				.filter(entity -> !entity.getId().equals(ownSectionId))
				.anyMatch(entity -> key.equalsIgnoreCase(entity.getSectionKey())))
			.ifPresent(key -> {
				throw Problem.valueOf(CONFLICT, SECTION_KEY_TAKEN.formatted(key, investigationEntity.getId()));
			});
	}

	private void logMissingIfMatch(final String ifMatch, final String method, final String namespace, final String municipalityId, final String errandId, final String investigationId) {
		if (ifMatch == null) {
			LOG.debug("{} /errands/{}/investigations/{} received without If-Match header (namespace={}, municipalityId={})", method, sanitizeForLogging(errandId), sanitizeForLogging(investigationId),
				sanitizeForLogging(namespace), sanitizeForLogging(municipalityId));
		}
	}
}

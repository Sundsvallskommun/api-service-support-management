package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.accessmapper.Access;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.dept44.support.Relation;
import se.sundsvall.supportmanagement.api.model.config.action.enums.OperationType;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.util.ErrandNumberGeneratorService;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;
import se.sundsvall.supportmanagement.integration.relation.RelationClient;
import se.sundsvall.supportmanagement.service.mapper.ErrandMapper;

import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrand;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandWithAccessControl;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandsWithAccessControl;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.updateEntity;
import static se.sundsvall.supportmanagement.service.util.ETagUtil.validateIfMatch;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

@Service
public class ErrandService {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandService.class);

	private static final String BAD_CONTACT_REASON = "'%s' is not a valid contact reason for namespace '%s' and municipality with id '%s'";
	private static final String EVENT_LOG_CREATE_ERRAND = "Ärendet har skapats.";
	private static final String EVENT_LOG_UPDATE_ERRAND = "Ärendet har uppdaterats.";
	private static final String EVENT_LOG_DELETE_ERRAND = "Ärendet har raderats.";

	private final ErrandsRepository repository;
	private final ContactReasonRepository contactReasonRepository;
	private final RevisionService revisionService;
	private final EventService eventService;
	private final ErrandNumberGeneratorService errandNumberGeneratorService;
	private final ErrandAttachmentService errandAttachmentService;
	private final CommunicationService communicationService;
	private final AttachmentRepository attachmentRepository;
	private final ConversationService conversationService;
	private final NotesClient notesClient;
	private final AccessControlService accessControlService;
	private final RelationClient relationClient;
	private final MetadataLabelRepository metadataLabelRepository;
	private final ErrandActionService errandActionService;
	private final ErrandPhaseService errandPhaseService;
	private final EntityManager entityManager;

	public ErrandService(
		final ErrandsRepository repository,
		final ContactReasonRepository contactReasonRepository,
		final CommunicationService communicationService,
		final AttachmentRepository attachmentRepository,
		final RevisionService revisionService,
		final EventService eventService,
		final ErrandNumberGeneratorService errandNumberGeneratorService,
		final ErrandAttachmentService errandAttachmentService,
		final ConversationService conversationService,
		final NotesClient notesClient,
		final AccessControlService accessControlService,
		final RelationClient relationClient,
		final MetadataLabelRepository metadataLabelRepository,
		final ErrandActionService errandActionService,
		final ErrandPhaseService errandPhaseService,
		final EntityManager entityManager) {

		this.repository = repository;
		this.contactReasonRepository = contactReasonRepository;
		this.communicationService = communicationService;
		this.attachmentRepository = attachmentRepository;
		this.revisionService = revisionService;
		this.eventService = eventService;
		this.errandNumberGeneratorService = errandNumberGeneratorService;
		this.errandAttachmentService = errandAttachmentService;
		this.conversationService = conversationService;
		this.notesClient = notesClient;
		this.accessControlService = accessControlService;
		this.relationClient = relationClient;
		this.metadataLabelRepository = metadataLabelRepository;
		this.errandActionService = errandActionService;
		this.errandPhaseService = errandPhaseService;
		this.entityManager = entityManager;
	}

	@Transactional
	public String createErrand(final String namespace, final String municipalityId, final Errand errand, final String referredFrom) {
		errand.withErrandNumber(errandNumberGeneratorService.generateErrandNumber(namespace, municipalityId));

		final var errandEntity = toErrandEntity(namespace, municipalityId, errand);
		Optional.ofNullable(errand.getContactReason()).ifPresent(reason -> {
			final var contactReason = contactReasonRepository.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(reason, namespace, municipalityId)
				.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, BAD_CONTACT_REASON.formatted(reason, namespace, municipalityId)));

			errandEntity
				.withContactReason(contactReason)
				.withContactReasonDescription(errand.getContactReasonDescription());
		});

		errandPhaseService.processPhaseChange(errandEntity, errand.getActivePhaseId(), namespace, municipalityId);
		errandPhaseService.validateStatusAgainstActivePhase(errandEntity, errandEntity.getStatus());

		computeAndSetAccessLabels(errandEntity);
		final var persistedEntity = repository.save(errandEntity);
		errandActionService.processErrandActions(persistedEntity, OperationType.CREATE);
		final var revision = revisionService.createErrandRevision(persistedEntity);

		try {
			eventService.createErrandEvent(CREATE, EVENT_LOG_CREATE_ERRAND, persistedEntity, revision.latest(), null, false, ERRAND);
		} catch (final Exception e) {
			LOG.warn("Failed to log CREATE event for errand {}: {}", persistedEntity.getId(), e.getMessage());
		}

		if (isNotBlank(referredFrom)) {
			final var relation = ErrandMapper.toReferredFromRelation(namespace, expandRelation(referredFrom), persistedEntity.getId());
			try {
				relationClient.createRelation(municipalityId, relation);
			} catch (final Exception e) {
				LOG.warn("Failed to create referredFrom relation for errand {}: {}", persistedEntity.getId(), e.getMessage());
			}
		}

		return persistedEntity.getId();
	}

	@Transactional(readOnly = true)
	public Page<Errand> findErrands(final String namespace, final String municipalityId, final Specification<ErrandEntity> filter, final Pageable pageable) {
		final var baseFilter = withNamespace(namespace).and(withMunicipalityId(municipalityId)).and(accessControlService.withAccessControl(namespace, municipalityId, Identifier.get()));
		final var fullFilter = ofNullable(filter).map(baseFilter::and).orElse(baseFilter);
		final var matches = repository.findAll(fullFilter, pageable);
		final var limitedMapping = accessControlService.limitedMappingPredicateByLabel(namespace, municipalityId, Identifier.get());

		return new PageImpl<>(toErrandsWithAccessControl(matches.getContent(), limitedMapping), pageable, matches.getTotalElements());
	}

	@Transactional(readOnly = true)
	public Errand readErrand(final String namespace, final String municipalityId, final String id) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, id, false);
		final var limitedMapping = accessControlService.limitedMappingPredicateByLabel(namespace, municipalityId, Identifier.get());
		return toErrandWithAccessControl(errandEntity, limitedMapping);
	}

	@Transactional
	public Errand updateErrand(final String namespace, final String municipalityId, final String id, final String ifMatch, final Errand errand) {
		final var errandEntityToUpdate = accessControlService.getErrand(namespace, municipalityId, id, true, Access.AccessLevelEnum.RW);

		if (ifMatch == null) {
			LOG.debug("PATCH /errands/{} received without If-Match header (namespace={}, municipalityId={})", sanitizeForLogging(id), sanitizeForLogging(namespace), sanitizeForLogging(municipalityId));
		}
		validateIfMatch(ifMatch, errandEntityToUpdate.getVersion());
		entityManager.lock(errandEntityToUpdate, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		final var errandEntity = updateEntity(errandEntityToUpdate, errand);

		errandPhaseService.processPhaseChange(errandEntity, errand.getActivePhaseId(), namespace, municipalityId);
		errandPhaseService.validateStatusAgainstActivePhase(errandEntity, errand.getStatus());

		Optional.ofNullable(errand.getContactReason()).ifPresent(reason -> {
			final var contactReason = contactReasonRepository.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(reason, namespace, municipalityId)
				.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, BAD_CONTACT_REASON.formatted(reason, namespace, municipalityId)));

			errandEntity.withContactReason(contactReason);
		});

		if (errand.getLabels() != null) {
			computeAndSetAccessLabels(errandEntity);
		}
		final var entity = repository.save(errandEntity);
		errandActionService.processErrandActions(entity, OperationType.UPDATE);

		final var revisionResult = revisionService.createErrandRevision(entity);

		if (nonNull(revisionResult)) {
			try {
				eventService.createErrandEvent(UPDATE, EVENT_LOG_UPDATE_ERRAND, entity, revisionResult.latest(), revisionResult.previous(), ERRAND);
			} catch (final Exception e) {
				LOG.warn("Failed to log UPDATE event for errand {}: {}", entity.getId(), e.getMessage());
			}
		}

		return toErrand(entity);
	}

	@Transactional
	public void deleteErrand(final String namespace, final String municipalityId, final String id, final String ifMatch) {
		final var entity = accessControlService.getErrand(namespace, municipalityId, id, true, Access.AccessLevelEnum.RW);

		if (ifMatch == null) {
			LOG.debug("DELETE /errands/{} received without If-Match header (namespace={}, municipalityId={})", sanitizeForLogging(id), sanitizeForLogging(namespace), sanitizeForLogging(municipalityId));
		}
		validateIfMatch(ifMatch, entity.getVersion());

		try {
			conversationService.deleteByErrandId(entity);
		} catch (final Exception e) {
			final var sanitizedId = sanitizeForLogging(id);
			LOG.warn("Failed to delete conversations for errand {}: {}", sanitizedId, e.getMessage());
		}

		communicationService.deleteAllCommunicationsByErrandNumber(entity.getErrandNumber());
		errandAttachmentService.readErrandAttachments(namespace, municipalityId, id)
			.forEach(attachment -> attachmentRepository.deleteById(attachment.getId()));

		try {
			final var notes = notesClient.findNotes(municipalityId, null, null, id, null, null, 1, 1000);
			notes.getNotes().forEach(note -> notesClient.deleteNoteById(municipalityId, note.getId()));
		} catch (final Exception e) {
			final var sanitizedId = sanitizeForLogging(id);
			LOG.warn("Failed to delete notes for errand {}: {}", sanitizedId, e.getMessage());
		}

		// Delete errand
		repository.deleteById(id);

		// Create a log event
		final var latestRevision = revisionService.getLatestErrandRevision(entity);
		try {
			eventService.createErrandEvent(DELETE, EVENT_LOG_DELETE_ERRAND, entity, latestRevision, null, false, ERRAND);
		} catch (final Exception e) {
			final var sanitizedId = sanitizeForLogging(id);
			LOG.warn("Failed to log DELETE event for errand {}: {}", sanitizedId, e.getMessage());
		}
	}

	@Transactional(readOnly = true)
	public Long countErrands(final String namespace, final String municipalityId, final Specification<ErrandEntity> filter) {
		final var baseFilter = withNamespace(namespace).and(withMunicipalityId(municipalityId)).and(accessControlService.withAccessControl(namespace, municipalityId, Identifier.get()));
		final var fullFilter = ofNullable(filter).map(baseFilter::and).orElse(baseFilter);
		return repository.count(fullFilter);
	}

	private void computeAndSetAccessLabels(final ErrandEntity errandEntity) {
		final var allLabelIds = ofNullable(errandEntity.getLabels())
			.orElse(emptyList())
			.stream()
			.map(ErrandLabelEmbeddable::getMetadataLabelId)
			.collect(Collectors.toSet());

		if (allLabelIds.isEmpty()) {
			errandEntity.setAccessLabels(new ArrayList<>());
			return;
		}

		// Repository lookup is needed because ErrandLabelEmbeddable's @ManyToOne metadataLabel
		// is only populated by Hibernate on load. For freshly created/updated labels (from the mapper),
		// getMetadataLabel() returns null.
		final var resourcePathById = metadataLabelRepository.findAllById(allLabelIds).stream()
			.collect(Collectors.toMap(MetadataLabelEntity::getId, MetadataLabelEntity::getResourcePath));

		final var ancestorIds = resourcePathById.entrySet().stream()
			.filter(entry -> resourcePathById.values().stream()
				.anyMatch(otherPath -> !otherPath.equals(entry.getValue()) && otherPath.startsWith(entry.getValue() + "/")))
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());

		final var accessLabels = allLabelIds.stream()
			.filter(id -> !ancestorIds.contains(id))
			.map(id -> AccessLabelEmbeddable.create().withMetadataLabelId(id))
			.collect(Collectors.toCollection(ArrayList::new));

		errandEntity.setAccessLabels(accessLabels);
	}

	se.sundsvall.dept44.support.Relation expandRelation(final String referredFromAsString) {
		final var relation = Relation.parseRelation(referredFromAsString);
		if (isNull(relation.getSource())) {
			throw Problem.valueOf(BAD_REQUEST,
				"Source information is missing in the referredFrom relation. Received: '%s'. The source must contain: sourceResourceId, sourceType, sourceService, and sourceNamespace. Expected format is '{relationType}|{sourceResourceId};{sourceType};{sourceService};{sourceNamespace}|'"
					.formatted(referredFromAsString));
		}
		return relation;
	}
}

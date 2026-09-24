package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
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
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.enums.OperationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.db.util.ErrandNumberGeneratorService;
import se.sundsvall.supportmanagement.integration.relation.RelationClient;
import se.sundsvall.supportmanagement.service.mapper.ErrandMapper;
import se.sundsvall.supportmanagement.service.model.ErrandEnrichment;
import se.sundsvall.supportmanagement.service.model.RevisionResult;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ErrandLifecycle.ACTIVE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ErrandLifecycle.DRAFT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandWithAccessControl;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandsWithAccessControl;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.updateEntity;
import static se.sundsvall.supportmanagement.service.util.ETagUtil.validateIfMatch;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withDefaultLifecycle;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

@Service
public class ErrandService {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandService.class);

	private static final String BAD_CONTACT_REASON = "'%s' is not a valid contact reason for namespace '%s' and municipality with id '%s'";
	private static final String ACTIVE_ERRAND_TO_DRAFT = "The errand '%s' is active, and an active errand never becomes a draft again";
	private static final String EVENT_LOG_CREATE_ERRAND = "Ärendet har skapats.";
	private static final String EVENT_LOG_UPDATE_ERRAND = "Ärendet har uppdaterats.";
	private static final String EVENT_LOG_ACTIVATE_ERRAND = "Ärendet har aktiverats.";
	private static final String EVENT_LOG_DELETE_ERRAND = "Ärendet har raderats.";
	private static final String LABELS_KEPT = "The labels of the errand were therefore not rebuilt after a label was moved, and it keeps the labels it had. Move the label back, or give the errand labels that name the process it runs.";

	private final ErrandsRepository repository;
	private final ContactReasonRepository contactReasonRepository;
	private final MeasureValidator measureValidator;
	private final RevisionService revisionService;
	private final EventService eventService;
	private final ErrandNumberGeneratorService errandNumberGeneratorService;
	private final ErrandAttachmentService errandAttachmentService;
	private final ErrandDataDeleter errandDataDeleter;
	private final AccessControlService accessControlService;
	private final RelationClient relationClient;
	private final ErrandLabelService errandLabelService;
	private final ErrandActionService errandActionService;
	private final ErrandPhaseService errandPhaseService;
	private final ErrandProcessService errandProcessService;
	private final ProcessKeyGuard processKeyGuard;
	private final DecisionValidator decisionValidator;
	private final EntityManager entityManager;

	public ErrandService(
		final ErrandsRepository repository,
		final ContactReasonRepository contactReasonRepository,
		final MeasureValidator measureValidator,
		final RevisionService revisionService,
		final EventService eventService,
		final ErrandNumberGeneratorService errandNumberGeneratorService,
		final ErrandAttachmentService errandAttachmentService,
		final ErrandDataDeleter errandDataDeleter,
		final AccessControlService accessControlService,
		final RelationClient relationClient,
		final ErrandLabelService errandLabelService,
		final ErrandActionService errandActionService,
		final ErrandPhaseService errandPhaseService,
		final ErrandProcessService errandProcessService,
		final ProcessKeyGuard processKeyGuard,
		final DecisionValidator decisionValidator,
		final EntityManager entityManager) {

		this.repository = repository;
		this.contactReasonRepository = contactReasonRepository;
		this.measureValidator = measureValidator;
		this.revisionService = revisionService;
		this.eventService = eventService;
		this.errandNumberGeneratorService = errandNumberGeneratorService;
		this.errandAttachmentService = errandAttachmentService;
		this.errandDataDeleter = errandDataDeleter;
		this.accessControlService = accessControlService;
		this.relationClient = relationClient;
		this.errandLabelService = errandLabelService;
		this.errandActionService = errandActionService;
		this.errandPhaseService = errandPhaseService;
		this.errandProcessService = errandProcessService;
		this.processKeyGuard = processKeyGuard;
		this.decisionValidator = decisionValidator;
		this.entityManager = entityManager;
	}

	@Transactional
	public String createErrand(final String namespace, final String municipalityId, final Errand errand, final String referredFrom) {
		errand.withErrandNumber(errandNumberGeneratorService.generateErrandNumber(namespace, municipalityId));

		// Everything the request is held to on its own, before an errand is built from it.
		measureValidator.validate(errand.getMeasures(), namespace, municipalityId);
		errandLabelService.validateLabels(namespace, municipalityId, errand.getLabels());
		final var contactReason = resolveContactReason(errand.getContactReason(), namespace, municipalityId);

		final var errandEntity = toErrandEntity(namespace, municipalityId, errand);
		ofNullable(contactReason).ifPresent(reason -> errandEntity
			.withContactReason(reason)
			.withContactReasonDescription(errand.getContactReasonDescription()));

		errandPhaseService.applyPhaseChange(errandEntity, errand.getActivePhaseId(), errandEntity.getStatus(), namespace, municipalityId);
		errandLabelService.settleAccessLabels(errandEntity);
		processKeyGuard.verifyNewLabels(errandEntity.getLabels());

		final var persistedEntity = repository.save(errandEntity);
		errandActionService.processErrandActions(persistedEntity, OperationType.CREATE);
		final var revision = revisionService.createErrandRevision(persistedEntity);

		logCreateEvent(persistedEntity, revision);

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
		final var baseFilter = withNamespace(namespace).and(withMunicipalityId(municipalityId)).and(withDefaultLifecycle(filter))
			.and(accessControlService.withAccessControl(namespace, municipalityId, Identifier.get(), ProtectedResource.ERRAND, LR));
		final var fullFilter = ofNullable(filter).map(baseFilter::and).orElse(baseFilter);
		final var matches = repository.findAll(fullFilter, pageable);
		final var fieldResolver = accessControlService.roleBasedFieldResolver(namespace, municipalityId, Identifier.get());

		return new PageImpl<>(toErrandsWithAccessControl(matches.getContent(), fieldResolver, enrichmentOf(namespace, municipalityId, matches.getContent())), pageable, matches.getTotalElements());
	}

	@Transactional(readOnly = true)
	public Errand readErrand(final String namespace, final String municipalityId, final String id) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, id, false, ProtectedResource.ERRAND, LR);
		final var fieldResolver = accessControlService.roleBasedFieldResolver(namespace, municipalityId, Identifier.get());
		return toErrandWithAccessControl(errandEntity, fieldResolver, enrichmentOf(namespace, municipalityId, List.of(errandEntity)));
	}

	/**
	 * What the errands of a page carry beyond their own rows, read in one query for the whole page.
	 */
	private ErrandEnrichment enrichmentOf(final String namespace, final String municipalityId, final List<ErrandEntity> entities) {
		return new ErrandEnrichment(errandProcessService.findLatestProcesses(namespace, municipalityId, entities.stream()
			.map(ErrandEntity::getId)
			.toList()));
	}

	@Transactional
	public Errand updateErrand(final String namespace, final String municipalityId, final String id, final String ifMatch, final Errand errand) {
		final var errandEntityToUpdate = accessControlService.getErrand(namespace, municipalityId, id, true, ProtectedResource.ERRAND, RW);

		// Verified and resolved before the errand is touched, so that patching it does not flush mid transaction, and so
		// that the response is mapped by the same grants a plain read of the errand would be.
		final var keyAccess = accessControlService.verifyKeyAccess(namespace, municipalityId, errandEntityToUpdate, errand);

		// Everything the patch is held to on its own, before the errand is touched by it.
		requireMatchingVersion(ifMatch, errandEntityToUpdate.getVersion(), id, namespace, municipalityId);
		requireLifecycleTransition(errandEntityToUpdate, errand);
		measureValidator.validate(errand.getMeasures(), namespace, municipalityId);
		errandLabelService.validateLabels(namespace, municipalityId, errand.getLabels());
		final var contactReason = resolveContactReason(errand.getContactReason(), namespace, municipalityId);

		final var labelsBeforePatch = nonNull(errand.getLabels())
			? List.copyOf(ofNullable(errandEntityToUpdate.getLabels()).orElse(emptyList()))
			: null;

		// Held now, since the patch is about to overwrite the life cycle it is judged from.
		final var activates = errandEntityToUpdate.isDraft() && ACTIVE.name().equals(errand.getLifecycle());

		entityManager.lock(errandEntityToUpdate, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		final var errandEntity = updateEntity(errandEntityToUpdate, errand, keyAccess.writableKey());
		ofNullable(contactReason).ifPresent(errandEntity::withContactReason);

		// Held against the status the errand ends up with rather than the one the patch carries, so that moving it into a
		// phase is judged by what its status will be and not only by whether the patch happens to name one.
		errandPhaseService.applyPhaseChange(errandEntity, errand.getActivePhaseId(), errandEntity.getStatus(), namespace, municipalityId);

		if (nonNull(errand.getLabels())) {
			errandLabelService.settleAccessLabels(errandEntity);
			processKeyGuard.verifyLabelChange(id, labelsBeforePatch, errandEntity.getLabels());
		}

		final var entity = repository.saveAndFlush(errandEntity);
		errandActionService.processErrandActions(entity, activates ? OperationType.CREATE : OperationType.UPDATE);
		logUpdateEvent(entity, revisionService.createErrandRevision(entity), activates ? EVENT_LOG_ACTIVATE_ERRAND : EVENT_LOG_UPDATE_ERRAND, true);

		return toErrandWithAccessControl(entity, keyAccess.readable(), enrichmentOf(namespace, municipalityId, List.of(entity)));
	}

	@Transactional
	public void deleteErrand(final String namespace, final String municipalityId, final String id, final String ifMatch) {
		final var entity = accessControlService.getErrand(namespace, municipalityId, id, true, ProtectedResource.ERRAND, RW);

		if (ifMatch == null) {
			LOG.debug("DELETE /errands/{} received without If-Match header (namespace={}, municipalityId={})", sanitizeForLogging(id), sanitizeForLogging(namespace), sanitizeForLogging(municipalityId));
		}
		validateIfMatch(ifMatch, entity.getVersion());
		decisionValidator.validateErrandRemovable(namespace, municipalityId, id);

		// Read before the removal, which takes the revisions with it, since the event written at the end of this method
		// points at the latest one. The event outlives the revision it names, which is the accepted cost of not keeping a
		// full snapshot of a deleted errand.
		final var latestRevision = revisionService.getLatestErrandRevision(entity);

		// Read for the same reason and at the same moment as the revision above. The removal empties the persistence
		// context to keep what it reads from filling the heap, which leaves the errand detached, and the event below is
		// built from its external tags - a collection that can no longer be loaded by then. Held here and put back, so
		// that a removal which happened is answered for rather than lost to a lazy collection.
		final var externalTags = List.copyOf(ofNullable(entity.getExternalTags()).orElse(emptyList()));

		// Attachments are read through the attachment service rather than off the entity, so that the access check
		// guarding them applies to a caller deleting them along with the errand.
		removeErrand(entity, errandAttachmentService.readErrandAttachments(namespace, municipalityId, id).stream()
			.map(ErrandAttachment::getId)
			.toList());

		entity.setExternalTags(externalTags);

		try {
			eventService.createErrandEvent(DELETE, EVENT_LOG_DELETE_ERRAND, entity, latestRevision, null, false, ERRAND);
		} catch (final Exception e) {
			final var sanitizedId = sanitizeForLogging(id);
			LOG.warn("Failed to log DELETE event for errand {}: {}", sanitizedId, e.getMessage());
		}
	}

	/**
	 * Removes an errand that has passed its retention period, along with everything belonging to it.
	 * <p>
	 * Called by the purge. Unlike {@link #deleteErrand(String, String, String, String)} no access check is made, no
	 * locked decision holds the removal back and no event is written. In a namespace with a process consumer the process
	 * consumer is told that the errand is gone, whether the errand had a process or not, and without a process key. What
	 * is removed is the same in both cases, and is held in {@link #removeErrand(ErrandEntity, List)}.
	 * <p>
	 * Runs in a transaction of its own, so that an errand that cannot be removed neither rolls back the errands already
	 * removed nor stops the run. An errand that is already gone is not an error, and is answered with false.
	 *
	 * @param  namespace      namespace of the errand.
	 * @param  municipalityId id of the municipality of the errand.
	 * @param  id             id of the errand to remove.
	 * @return                true if the errand was there to be removed, false if it was already gone.
	 */
	@Transactional(propagation = REQUIRES_NEW)
	public boolean purgeErrand(final String namespace, final String municipalityId, final String id) {
		final var entity = repository.findByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId).orElse(null);

		if (isNull(entity)) {
			return false;
		}

		// Taken straight off the entity, since a purge runs with no caller to authorize.
		removeErrand(entity, ofNullable(entity.getAttachments()).orElse(emptyList()).stream()
			.map(AttachmentEntity::getId)
			.toList());

		eventService.publishDeletionToProcess(entity);

		return true;
	}

	/**
	 * Removes an errand: everything hanging off it, its revisions and the errand row itself. Used by both the single
	 * errand delete and the retention purge.
	 *
	 * @param entity        the errand to remove.
	 * @param attachmentIds ids of the attachments to remove along with it.
	 */
	private void removeErrand(final ErrandEntity entity, final List<String> attachmentIds) {
		errandDataDeleter.deleteRelatedData(entity, attachmentIds);

		revisionService.deleteErrandRevisions(entity.getNamespace(), entity.getMunicipalityId(), entity.getId());

		repository.deleteById(entity.getId());
	}

	@Transactional(readOnly = true)
	public Long countErrands(final String namespace, final String municipalityId, final Specification<ErrandEntity> filter) {
		final var baseFilter = withNamespace(namespace).and(withMunicipalityId(municipalityId)).and(withDefaultLifecycle(filter))
			.and(accessControlService.withAccessControl(namespace, municipalityId, Identifier.get(), ProtectedResource.ERRAND, LR));
		final var fullFilter = ofNullable(filter).map(baseFilter::and).orElse(baseFilter);
		return repository.count(fullFilter);
	}

	/**
	 * Puts labels rebuilt after a label was moved on an errand, and settles its access labels from them.
	 * <p>
	 * A change that would move the errand off the process it runs is refused: the errand keeps the labels it has, and an
	 * error entry on it says why. A change made is recorded as a revision and an update event, without a notification.
	 *
	 * @param  entity the errand to relabel.
	 * @param  labels the labels it is to wear, ancestors included.
	 * @return        true when the labels were put on the errand, false when the change was refused.
	 */
	boolean persistLabelUpdate(final ErrandEntity entity, final List<ErrandLabelEmbeddable> labels) {
		final var labelsBefore = List.copyOf(ofNullable(entity.getLabels()).orElse(emptyList()));

		if (processKeyGuard.refusesLabelChange(entity.getId(), labelsBefore, labels, LABELS_KEPT)) {
			return false;
		}

		entity.setLabels(labels);
		errandLabelService.settleAccessLabels(entity);

		final var saved = repository.saveAndFlush(entity);
		logUpdateEvent(saved, revisionService.createErrandRevision(saved), EVENT_LOG_UPDATE_ERRAND, false);

		return true;
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

	/**
	 * The contact reason of sent in name, or null when the request names none.
	 */
	private ContactReasonEntity resolveContactReason(final String reason, final String namespace, final String municipalityId) {
		if (isNull(reason)) {
			return null;
		}

		return contactReasonRepository.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(reason, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, BAD_CONTACT_REASON.formatted(reason, namespace, municipalityId)));
	}

	/**
	 * Holds the errand to the version the caller believes it is at, noting the requests that leave it to chance.
	 */
	private void requireMatchingVersion(final String ifMatch, final Long version, final String id, final String namespace, final String municipalityId) {
		if (isNull(ifMatch)) {
			LOG.debug("PATCH /errands/{} received without If-Match header (namespace={}, municipalityId={})", sanitizeForLogging(id), sanitizeForLogging(namespace), sanitizeForLogging(municipalityId));
		}

		validateIfMatch(ifMatch, version);
	}

	/**
	 * Logs the errand having been created. A log that cannot be written is logged as a warning and does not fail the
	 * request.
	 */
	private void logCreateEvent(final ErrandEntity entity, final RevisionResult revision) {
		try {
			eventService.createErrandEvent(CREATE, EVENT_LOG_CREATE_ERRAND, entity, revision.latest(), null, false, ERRAND);
		} catch (final Exception e) {
			LOG.warn("Failed to log CREATE event for errand {}: {}", entity.getId(), e.getMessage());
		}
	}

	/**
	 * Refuses a patch that would make an active errand a draft again.
	 */
	private static void requireLifecycleTransition(final ErrandEntity entity, final Errand patch) {
		if (ACTIVE == entity.getLifecycle() && DRAFT.name().equals(patch.getLifecycle())) {
			throw Problem.valueOf(BAD_REQUEST, ACTIVE_ERRAND_TO_DRAFT.formatted(entity.getId()));
		}
	}

	/**
	 * Logs the errand having been updated, for the revisions that produced one.
	 */
	private void logUpdateEvent(final ErrandEntity entity, final RevisionResult revisionResult, final String message, final boolean sendNotification) {
		if (isNull(revisionResult)) {
			return;
		}

		try {
			eventService.createErrandEvent(UPDATE, message, entity, revisionResult.latest(), revisionResult.previous(), sendNotification, ERRAND);
		} catch (final Exception e) {
			LOG.warn("Failed to log UPDATE event for errand {}: {}", entity.getId(), e.getMessage());
		}
	}
}

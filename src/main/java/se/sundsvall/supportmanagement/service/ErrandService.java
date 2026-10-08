package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.dept44.support.Relation;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.enums.OperationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.db.util.AttachmentSequenceNumberGenerator;
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
import static se.sundsvall.supportmanagement.service.mapper.LabelClassificationMapper.applyClassificationDisplayNames;
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
	private static final String LABELS_KEPT = "The labels of the errand were therefore not rebuilt after labels were moved or merged, and it keeps the labels it had. Give the errand labels that name the process it runs, or put the labels back as they were.";
	private static final String LABELS_NOT_REBUILT = "The labels of the errand were therefore not rebuilt after labels were moved or merged, and it keeps the labels it had.";

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
	private final ProcessBlockGuard processBlockGuard;
	private final DecisionValidator decisionValidator;
	private final LabelClassificationService labelClassificationService;
	private final EntityManager entityManager;
	private final AttachmentSequenceNumberGenerator attachmentSequenceNumberGenerator;

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
		final ProcessBlockGuard processBlockGuard,
		final DecisionValidator decisionValidator,
		final LabelClassificationService labelClassificationService,
		final EntityManager entityManager,
		final AttachmentSequenceNumberGenerator attachmentSequenceNumberGenerator) {

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
		this.processBlockGuard = processBlockGuard;
		this.decisionValidator = decisionValidator;
		this.labelClassificationService = labelClassificationService;
		this.entityManager = entityManager;
		this.attachmentSequenceNumberGenerator = attachmentSequenceNumberGenerator;
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
		attachmentSequenceNumberGenerator.startSequence(persistedEntity);
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

		final var displayNames = labelClassificationService.getClassificationDisplayNames(namespace, municipalityId);
		final var errands = toErrandsWithAccessControl(matches.getContent(), fieldResolver, enrichmentOf(namespace, municipalityId, matches.getContent())).stream()
			.map(errand -> applyClassificationDisplayNames(errand, displayNames))
			.toList();

		return new PageImpl<>(errands, pageable, matches.getTotalElements());
	}

	@Transactional(readOnly = true)
	public Errand readErrand(final String namespace, final String municipalityId, final String id) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, id, false, ProtectedResource.ERRAND, LR);
		final var fieldResolver = accessControlService.roleBasedFieldResolver(namespace, municipalityId, Identifier.get());
		return applyClassificationDisplayNames(toErrandWithAccessControl(errandEntity, fieldResolver, enrichmentOf(namespace, municipalityId, List.of(errandEntity))),
			labelClassificationService.getClassificationDisplayNames(namespace, municipalityId));
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

		// Read before the errand is touched, so that failing to read them cannot roll back an update whose actions and event
		// have already gone out.
		final var classificationDisplayNames = labelClassificationService.getClassificationDisplayNames(namespace, municipalityId);

		entityManager.lock(errandEntityToUpdate, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		final var errandEntity = updateEntity(errandEntityToUpdate, errand, keyAccess.writableKey());
		ofNullable(contactReason).ifPresent(errandEntity::withContactReason);

		// Held against the status the errand ends up with rather than the one the patch carries, so that moving it into a
		// phase is judged by what its status will be and not only by whether the patch happens to name one.
		errandPhaseService.applyPhaseChange(errandEntity, errand.getActivePhaseId(), errandEntity.getStatus(), namespace, municipalityId);

		if (nonNull(errand.getLabels())) {
			errandLabelService.settleAccessLabels(errandEntity);
			processBlockGuard.verifyLabelChange(id, labelsBeforePatch, errandEntity.getLabels());
			processKeyGuard.verifyLabelChange(id, labelsBeforePatch, errandEntity.getLabels());
		}

		final var entity = repository.saveAndFlush(errandEntity);
		errandActionService.processErrandActions(entity, activates ? OperationType.CREATE : OperationType.UPDATE);
		logUpdateEvent(entity, revisionService.createErrandRevision(entity), activates ? EVENT_LOG_ACTIVATE_ERRAND : EVENT_LOG_UPDATE_ERRAND, true);

		return applyClassificationDisplayNames(toErrandWithAccessControl(entity, keyAccess.readable(), enrichmentOf(namespace, municipalityId, List.of(entity))), classificationDisplayNames);
	}

	@Transactional
	public void deleteErrand(final String namespace, final String municipalityId, final String id, final String ifMatch) {
		final var entity = accessControlService.getErrand(namespace, municipalityId, id, true, ProtectedResource.ERRAND, RW);

		if (ifMatch == null && LOG.isDebugEnabled()) {
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
		final var labels = labelIdsOf(entity);

		// Attachments are read through the attachment service rather than off the entity, so that the access check
		// guarding them applies to a caller deleting them along with the errand.
		removeErrand(entity, errandAttachmentService.readErrandAttachments(namespace, municipalityId, id).stream()
			.map(ErrandAttachment::getId)
			.toList());

		entity.setExternalTags(externalTags);
		entity.setLabels(labels);

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
	 * consumer is told that the errand is gone, whether the errand had a process or not, unless a label of the errand
	 * blocks processes. The deletion carries the key of the process of the errand when it has one, and no key when it has
	 * none. What is removed is the same in both cases, and is held in {@link #removeErrand(ErrandEntity, List)}.
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

		final var labels = labelIdsOf(entity);

		// Taken straight off the entity, since a purge runs with no caller to authorize.
		removeErrand(entity, ofNullable(entity.getAttachments()).orElse(emptyList()).stream()
			.map(AttachmentEntity::getId)
			.toList());

		entity.setLabels(labels);
		eventService.publishDeletionToProcess(entity);

		return true;
	}

	/**
	 * The labels of an errand about to be removed, by id only, to be put back on it once it is gone. The deletion is held
	 * back from the process by them when one of them blocks processes, and the removal leaves the errand and the metadata
	 * labels it points at detached, so the labels are looked up again by id rather than read off them.
	 */
	private static List<ErrandLabelEmbeddable> labelIdsOf(final ErrandEntity entity) {
		return ofNullable(entity.getLabels()).orElse(emptyList()).stream()
			.map(label -> ErrandLabelEmbeddable.create().withMetadataLabelId(label.getMetadataLabelId()))
			.toList();
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
	 * Rebuilds the labels of a batch of errands from their access labels after a label was moved, each through
	 * {@link #persistLabelUpdate(ErrandEntity, List, boolean)}, in a transaction of its own.
	 * <p>
	 * The errands are to have been read with their access labels in a transaction that has ended, and nothing else on them
	 * is read. An errand with labels but no access labels is left as it is.
	 *
	 * @param batch              the errands to relabel.
	 * @param startedByAdAccount whether the job moving the label was started by an ad account.
	 */
	@Transactional(propagation = REQUIRES_NEW)
	void persistLabelMigrationBatch(final List<ErrandEntity> batch, final boolean startedByAdAccount) {
		final var idsWithLabels = idsWithNonEmptyLabels(batch);
		batch.forEach(errand -> restowFromAccessLabels(errand, idsWithLabels, startedByAdAccount));
	}

	private void restowFromAccessLabels(final ErrandEntity errand, final Set<String> idsWithLabels, final boolean startedByAdAccount) {
		if (skipIfAccessLabelsMissing(errand, idsWithLabels)) {
			return;
		}

		final var leafLabels = ofNullable(errand.getAccessLabels()).orElse(emptyList()).stream()
			.map(accessLabel -> ErrandLabelEmbeddable.create().withMetadataLabelId(accessLabel.getMetadataLabelId()))
			.toList();

		persistLabelUpdate(errand, leafLabels, startedByAdAccount);
	}

	/**
	 * Rebuilds the labels of a batch of errands after labels were merged, as
	 * {@link #persistLabelMigrationBatch(List, boolean)} does, with every access label among {@code sourceLabelIds}
	 * replaced by {@code targetLabelId} first.
	 *
	 * @param batch              the errands to relabel.
	 * @param sourceLabelIds     the labels merged into the target.
	 * @param targetLabelId      the label the sources were merged into.
	 * @param startedByAdAccount whether the job merging the labels was started by an ad account.
	 */
	@Transactional(propagation = REQUIRES_NEW)
	void persistLabelMergeBatch(final List<ErrandEntity> batch, final Set<String> sourceLabelIds, final String targetLabelId, final boolean startedByAdAccount) {
		final var idsWithLabels = idsWithNonEmptyLabels(batch);
		batch.forEach(errand -> restowFromAccessLabelsWithSubstitution(errand, sourceLabelIds, targetLabelId, idsWithLabels, startedByAdAccount));
	}

	private void restowFromAccessLabelsWithSubstitution(final ErrandEntity errand, final Set<String> sourceLabelIds, final String targetLabelId, final Set<String> idsWithLabels,
		final boolean startedByAdAccount) {

		if (skipIfAccessLabelsMissing(errand, idsWithLabels)) {
			return;
		}

		final var leafLabels = ofNullable(errand.getAccessLabels()).orElse(emptyList()).stream()
			.map(AccessLabelEmbeddable::getMetadataLabelId)
			.map(leafId -> sourceLabelIds.contains(leafId) ? targetLabelId : leafId)
			.distinct()
			.map(leafId -> ErrandLabelEmbeddable.create().withMetadataLabelId(leafId))
			.toList();

		persistLabelUpdate(errand, leafLabels, startedByAdAccount);
	}

	/**
	 * The ids of the errands in the batch whose labels are not empty, read in one query, as the labels of an errand read in
	 * a transaction that has ended cannot be loaded.
	 */
	private Set<String> idsWithNonEmptyLabels(final List<ErrandEntity> batch) {
		return repository.findIdsWithNonEmptyLabels(batch.stream().map(ErrandEntity::getId).toList());
	}

	/**
	 * Whether the errand has labels but no access labels, which a rebuild from its access labels would wipe. Such an errand
	 * is logged and left as it is.
	 */
	private boolean skipIfAccessLabelsMissing(final ErrandEntity errand, final Set<String> idsWithLabels) {
		final var hasAccessLabels = !ofNullable(errand.getAccessLabels()).orElse(emptyList()).isEmpty();

		if (idsWithLabels.contains(errand.getId()) && !hasAccessLabels) {
			LOG.warn("Errand {} has labels but no access labels - skipping restow rather than clearing its label set", sanitizeForLogging(errand.getId()));
			return true;
		}
		return false;
	}

	/**
	 * Puts labels rebuilt after labels were moved or merged on an errand, expanded to their ancestors, and settles its
	 * access labels from them.
	 * <p>
	 * Runs in the transaction of the batch, on an errand read before it. An errand that has been changed or removed since
	 * it was read is answered with {@link ObjectOptimisticLockingFailureException}, so that the batch is retried on a fresh
	 * read. A change is refused when it would take a label blocking processes off the errand and the job was started by an
	 * ad account, and when it would move the errand off the process it runs: the errand keeps the labels it has, and an
	 * error entry on it says why. A change made is recorded as a revision and an update event, without a notification.
	 *
	 * @param errand             the errand to relabel, as read before the transaction.
	 * @param labels             the labels it is to wear, without their ancestors.
	 * @param startedByAdAccount whether the job making the change was started by an ad account.
	 */
	void persistLabelUpdate(final ErrandEntity errand, final List<ErrandLabelEmbeddable> labels, final boolean startedByAdAccount) {
		final var stored = repository.findById(errand.getId())
			.filter(current -> Objects.equals(current.getVersion(), errand.getVersion()))
			.orElseThrow(() -> new ObjectOptimisticLockingFailureException(ErrandEntity.class, errand.getId()));
		final var labelsBefore = List.copyOf(ofNullable(stored.getLabels()).orElse(emptyList()));

		errand.setLabels(labels);
		errandLabelService.settleAccessLabels(errand);

		if (processBlockGuard.refusesLabelChange(errand.getId(), labelsBefore, errand.getLabels(), startedByAdAccount, LABELS_NOT_REBUILT)
			|| processKeyGuard.refusesLabelChange(errand.getId(), labelsBefore, errand.getLabels(), LABELS_KEPT)) {
			return;
		}

		final var saved = repository.saveAndFlush(errand);
		logUpdateEvent(saved, revisionService.createErrandRevision(saved), EVENT_LOG_UPDATE_ERRAND, false);
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
		if (isNull(ifMatch) && LOG.isDebugEnabled()) {
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
	 *
	 * @param notifies whether the event notifies the handler and the subscribers of the errand, or no one.
	 */
	private void logUpdateEvent(final ErrandEntity entity, final RevisionResult revisionResult, final String message, final boolean notifies) {
		if (isNull(revisionResult)) {
			return;
		}

		try {
			if (notifies) {
				eventService.createErrandEvent(UPDATE, message, entity, revisionResult.latest(), revisionResult.previous(), true, ERRAND);
			} else {
				eventService.createErrandEventWithoutNotification(UPDATE, message, entity, revisionResult.latest(), revisionResult.previous(), ERRAND);
			}
		} catch (final Exception e) {
			LOG.warn("Failed to log UPDATE event for errand {}: {}", entity.getId(), e.getMessage());
		}
	}
}

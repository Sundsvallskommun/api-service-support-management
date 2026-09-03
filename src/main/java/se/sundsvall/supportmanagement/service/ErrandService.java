package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachment;
import se.sundsvall.supportmanagement.api.model.config.action.enums.OperationType;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ErrandLabel;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.integration.db.util.ErrandNumberGeneratorService;
import se.sundsvall.supportmanagement.integration.relation.RelationClient;
import se.sundsvall.supportmanagement.service.mapper.ErrandMapper;

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
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;
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
	private final MeasureValidator measureValidator;
	private final RevisionService revisionService;
	private final EventService eventService;
	private final ErrandNumberGeneratorService errandNumberGeneratorService;
	private final ErrandAttachmentService errandAttachmentService;
	private final ErrandDataDeleter errandDataDeleter;
	private final AccessControlService accessControlService;
	private final RelationClient relationClient;
	private final MetadataLabelRepository metadataLabelRepository;
	private final ErrandActionService errandActionService;
	private final ErrandPhaseService errandPhaseService;
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
		final MetadataLabelRepository metadataLabelRepository,
		final ErrandActionService errandActionService,
		final ErrandPhaseService errandPhaseService,
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

		measureValidator.validate(errand.getMeasures(), namespace, municipalityId);

		errandPhaseService.processPhaseChange(errandEntity, errand.getActivePhaseId(), namespace, municipalityId);
		errandPhaseService.validateStatusAgainstActivePhase(errandEntity, errandEntity.getStatus());

		validateLabelVersions(errand.getLabels());
		expandLabelsToAncestorChain(errandEntity);
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
		final var baseFilter = withNamespace(namespace).and(withMunicipalityId(municipalityId)).and(accessControlService.withAccessControl(namespace, municipalityId, Identifier.get(), ProtectedResource.ERRAND, LR));
		final var fullFilter = ofNullable(filter).map(baseFilter::and).orElse(baseFilter);
		final var matches = repository.findAll(fullFilter, pageable);
		final var fieldResolver = accessControlService.roleBasedFieldResolver(namespace, municipalityId, Identifier.get());

		return new PageImpl<>(toErrandsWithAccessControl(matches.getContent(), fieldResolver), pageable, matches.getTotalElements());
	}

	@Transactional(readOnly = true)
	public Errand readErrand(final String namespace, final String municipalityId, final String id) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, id, false, ProtectedResource.ERRAND, LR);
		final var fieldResolver = accessControlService.roleBasedFieldResolver(namespace, municipalityId, Identifier.get());
		return toErrandWithAccessControl(errandEntity, fieldResolver);
	}

	@Transactional
	public Errand updateErrand(final String namespace, final String municipalityId, final String id, final String ifMatch, final Errand errand) {
		final var errandEntityToUpdate = accessControlService.getErrand(namespace, municipalityId, id, true, ProtectedResource.ERRAND, RW);

		// Resolved before the errand is touched, so that patching it does not flush mid transaction, and so that the
		// response is mapped by the same grants a plain read of the errand would be.
		final var fieldResolver = accessControlService.roleBasedFieldResolver(namespace, municipalityId, Identifier.get());
		final var accessibleKey = accessControlService.readableKeyResolver(namespace, municipalityId, Identifier.get(), errandEntityToUpdate);

		// A key the caller cannot read is a key they cannot write, whichever endpoint they write it through.
		accessControlService.verifyAccessibleKeys(accessibleKey.apply(ErrandField.PARAMETERS), keysOf(errand.getParameters(), Parameter::getKey));
		accessControlService.verifyAccessibleKeys(accessibleKey.apply(ErrandField.JSON_PARAMETERS), keysOf(errand.getJsonParameters(), JsonParameter::getKey));
		accessControlService.verifyAccessibleKeys(accessibleKey.apply(ErrandField.EXTERNAL_TAGS), keysOf(errand.getExternalTags(), ExternalTag::getKey));

		if (ifMatch == null) {
			LOG.debug("PATCH /errands/{} received without If-Match header (namespace={}, municipalityId={})", sanitizeForLogging(id), sanitizeForLogging(namespace), sanitizeForLogging(municipalityId));
		}
		validateIfMatch(ifMatch, errandEntityToUpdate.getVersion());
		if (errand.getMeasures() != null) {
			accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, id, ProtectedResource.MEASURE, RW);
		}
		entityManager.lock(errandEntityToUpdate, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		measureValidator.validate(errand.getMeasures(), errandEntityToUpdate.getMeasures(), namespace, municipalityId);

		final var errandEntity = updateEntity(errandEntityToUpdate, errand, accessibleKey);

		errandPhaseService.processPhaseChange(errandEntity, errand.getActivePhaseId(), namespace, municipalityId);
		errandPhaseService.validateStatusAgainstActivePhase(errandEntity, errand.getStatus());

		Optional.ofNullable(errand.getContactReason()).ifPresent(reason -> {
			final var contactReason = contactReasonRepository.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(reason, namespace, municipalityId)
				.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, BAD_CONTACT_REASON.formatted(reason, namespace, municipalityId)));

			errandEntity.withContactReason(contactReason);
		});

		if (errand.getLabels() != null) {
			validateLabelVersions(errand.getLabels());
			expandLabelsToAncestorChain(errandEntity);
			computeAndSetAccessLabels(errandEntity);
		}
		final var entity = repository.saveAndFlush(errandEntity);
		errandActionService.processErrandActions(entity, OperationType.UPDATE);

		final var revisionResult = revisionService.createErrandRevision(entity);

		if (nonNull(revisionResult)) {
			try {
				eventService.createErrandEvent(UPDATE, EVENT_LOG_UPDATE_ERRAND, entity, revisionResult.latest(), revisionResult.previous(), ERRAND);
			} catch (final Exception e) {
				LOG.warn("Failed to log UPDATE event for errand {}: {}", entity.getId(), e.getMessage());
			}
		}

		return toErrandWithAccessControl(entity, fieldResolver);
	}

	/**
	 * Keys of a keyed collection of a patch, or null when the patch leaves the collection alone.
	 */
	private static <T> List<String> keysOf(final List<T> values, final Function<T, String> keyExtractor) {
		return ofNullable(values)
			.map(list -> list.stream().map(keyExtractor).toList())
			.orElse(null);
	}

	@Transactional
	public void deleteErrand(final String namespace, final String municipalityId, final String id, final String ifMatch) {
		final var entity = accessControlService.getErrand(namespace, municipalityId, id, true, ProtectedResource.ERRAND, RW);

		if (ifMatch == null) {
			LOG.debug("DELETE /errands/{} received without If-Match header (namespace={}, municipalityId={})", sanitizeForLogging(id), sanitizeForLogging(namespace), sanitizeForLogging(municipalityId));
		}
		validateIfMatch(ifMatch, entity.getVersion());

		// Read before the removal, which takes the revisions with it, since the event written at the end of this method
		// points at the latest one. The event outlives the revision it names, which is the accepted cost of not keeping a
		// full snapshot of a deleted errand.
		final var latestRevision = revisionService.getLatestErrandRevision(entity);

		// Attachments are read through the attachment service rather than off the entity, so that the access check
		// guarding them applies to a caller deleting them along with the errand.
		removeErrand(entity, errandAttachmentService.readErrandAttachments(namespace, municipalityId, id).stream()
			.map(ErrandAttachment::getId)
			.toList());

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
	 * Called by the purge, which runs on a cutoff rather than on behalf of a caller, and therefore differs from
	 * {@link #deleteErrand(String, String, String, String)} on two points. There is no user to authorize, so no access
	 * check is made. And no event is written: an event per removed errand would cost a remote call for every one of them
	 * and would leave behind a record of the very errand the purge exists to remove. What is removed is the same in both
	 * cases, and is held in {@link #removeErrand(ErrandEntity, List)}.
	 * <p>
	 * Runs in a transaction of its own, so that an errand that cannot be removed neither rolls back the errands already
	 * removed nor stops the run. An errand that is already gone is not an error - it is the outcome the purge wanted -
	 * but it was not this call that removed it, which is what the answer distinguishes. That matters for the counters of
	 * a run: an errand deleted by a caller, or by a second purge on another instance, between the batch being read and
	 * this call must not be counted as removed twice.
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

		return true;
	}

	/**
	 * Removes an errand: everything hanging off it, its revisions and the errand row itself.
	 * <p>
	 * Shared by the single errand delete and by the retention purge. The two differ on what surrounds a removal - who is
	 * authorized, what is logged, which transaction it runs in and where the attachment ids come from - but not on what
	 * is removed, and holding that in one place is what keeps them from drifting apart.
	 * <p>
	 * The revisions go with the errand in both cases, since each of them holds a full serialized snapshot of it and
	 * leaving them behind would keep a complete copy of what the removal set out to remove.
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
		final var baseFilter = withNamespace(namespace).and(withMunicipalityId(municipalityId)).and(accessControlService.withAccessControl(namespace, municipalityId, Identifier.get(), ProtectedResource.ERRAND, LR));
		final var fullFilter = ofNullable(filter).map(baseFilter::and).orElse(baseFilter);
		return repository.count(fullFilter);
	}

	void validateLabelVersions(final List<ErrandLabel> labels) {
		var labelsWithVersion = ofNullable(labels).orElse(emptyList()).stream()
			.filter(label -> label.getVersion() != null)
			.toList();

		if (labelsWithVersion.isEmpty()) {
			return;
		}

		var labelIds = labelsWithVersion.stream().map(ErrandLabel::getId).toList();
		var currentVersionById = metadataLabelRepository.findAllById(labelIds).stream()
			.collect(HashMap::new, (m, e) -> m.put(e.getId(), e.getVersion()), HashMap::putAll);

		labelsWithVersion.stream()
			.filter(label -> {
				var current = currentVersionById.get(label.getId());
				return current != null && !current.equals(label.getVersion());
			})
			.findFirst()
			.ifPresent(label -> {
				throw Problem.valueOf(PRECONDITION_FAILED,
					"Label with id '%s' has been modified — expected version %d but current version is %d"
						.formatted(label.getId(), label.getVersion(), currentVersionById.get(label.getId())));
			});
	}

	void expandLabelsToAncestorChain(final ErrandEntity errandEntity) {
		var currentIds = ofNullable(errandEntity.getLabels()).orElse(emptyList()).stream()
			.map(ErrandLabelEmbeddable::getMetadataLabelId)
			.collect(Collectors.toSet());

		if (currentIds.isEmpty()) {
			return;
		}

		var ancestorPaths = metadataLabelRepository.findAllById(currentIds).stream()
			.map(MetadataLabelEntity::getResourcePath)
			.flatMap(path -> ancestorResourcePaths(path).stream())
			.collect(Collectors.toSet());

		if (ancestorPaths.isEmpty()) {
			return;
		}

		var missingAncestors = metadataLabelRepository
			.findByNamespaceAndMunicipalityIdAndResourcePathIn(errandEntity.getNamespace(), errandEntity.getMunicipalityId(), ancestorPaths)
			.stream()
			.filter(a -> !currentIds.contains(a.getId()))
			.map(a -> ErrandLabelEmbeddable.create().withMetadataLabelId(a.getId()))
			.toList();

		if (missingAncestors.isEmpty()) {
			return;
		}

		var expanded = new ArrayList<>(errandEntity.getLabels());
		expanded.addAll(missingAncestors);
		errandEntity.setLabels(expanded);
	}

	private static Set<String> ancestorResourcePaths(final String resourcePath) {
		var parts = resourcePath.split("/");
		var paths = new HashSet<String>();
		var sb = new StringBuilder();
		for (int i = 0; i < parts.length - 1; i++) {
			if (i > 0) {
				sb.append("/");
			}
			sb.append(parts[i]);
			paths.add(sb.toString());
		}
		return paths;
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

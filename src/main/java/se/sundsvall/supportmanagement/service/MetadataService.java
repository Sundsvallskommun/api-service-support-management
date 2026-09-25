package se.sundsvall.supportmanagement.service;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.api.model.metadata.AffectedAction;
import se.sundsvall.supportmanagement.api.model.metadata.AttachmentPurpose;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.api.model.metadata.DecisionOutcome;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.LabelMoveDryRunResponse;
import se.sundsvall.supportmanagement.api.model.metadata.LabelMoveRequest;
import se.sundsvall.supportmanagement.api.model.metadata.Labels;
import se.sundsvall.supportmanagement.api.model.metadata.MeasureType;
import se.sundsvall.supportmanagement.api.model.metadata.MetadataResponse;
import se.sundsvall.supportmanagement.api.model.metadata.Phase;
import se.sundsvall.supportmanagement.api.model.metadata.PhaseTransition;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.api.model.metadata.StatementOutcome;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.config.JobProperties;
import se.sundsvall.supportmanagement.integration.db.ActionConfigRepository;
import se.sundsvall.supportmanagement.integration.db.AttachmentPurposeRepository;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.CategoryRepository;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.DecisionOutcomeRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.ExternalIdTypeRepository;
import se.sundsvall.supportmanagement.integration.db.MeasureTypeRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.PhaseRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;
import se.sundsvall.supportmanagement.integration.db.StatementOutcomeRepository;
import se.sundsvall.supportmanagement.integration.db.StatusRepository;
import se.sundsvall.supportmanagement.integration.db.ValidationRepository;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureTypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.ValidationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;
import se.sundsvall.supportmanagement.service.mapper.MetadataMapper;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.CollectionUtils.isEmpty;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.MOVE_LABEL;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toAttachmentPurpose;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toAttachmentPurposeEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toCategory;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toCategoryEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toContactReason;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toContactReasonEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toDecisionOutcome;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toDecisionOutcomeEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toExternalIdType;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toExternalIdTypeEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toLabels;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toMeasureType;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toMeasureTypeEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toMetadataLabelEntityList;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toPhase;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toPhaseEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toPhaseTransitionEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toRole;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toRoleEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toStatementOutcome;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toStatementOutcomeEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toStatus;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toStatusEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateAttachmentPurposeEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateContactReason;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateDecisionOutcomeEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateExternalIdTypeEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateMeasureTypeEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateMetadataLabelEntities;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updatePhaseEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateRoleEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateStatementOutcomeEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateStatusEntity;

@Service
public class MetadataService {

	private static final String MEASURE_TYPE_WITHOUT_GROUP = "A measure type must belong to at least one group";
	private static final String ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID = "%s '%s' already exists in namespace '%s' for municipalityId '%s'";
	private static final String ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID = "%s '%s' is not present in namespace '%s' for municipalityId '%s'";
	private static final String LABEL = "Label";
	private static final String HAS_LABEL = "hasLabel";
	private static final String MOVE_ALREADY_IN_PROGRESS = "A job is already running for namespace '%s' in municipality with id '%s'";
	private static final String COULD_NOT_START = "Label move could not be started: %s";
	private static final String UNKNOWN_CALLER = "unknown";
	private static final int RESOURCE_PATH_MAX_LENGTH = 255;

	private static final String CONTACT_REASON = "ContactReason";
	private static final String CATEGORY = "Category";
	private static final String DECISION_OUTCOME = "DecisionOutcome";
	private static final String EXTERNAL_ID_TYPE = "ExternalIdType";
	private static final String PHASE = "Phase";
	private static final String PHASE_TRANSITION = "PhaseTransition";
	private static final String MEASURE_TYPE = "MeasureType";
	private static final String ATTACHMENT_PURPOSE = "AttachmentPurpose";
	private static final String ATTACHMENT_PURPOSE_IN_USE = "AttachmentPurpose '%s' cannot be deleted because it is referenced by one or more attachments";
	private static final String ROLE = "Role";
	private static final String STATEMENT_OUTCOME = "StatementOutcome";
	private static final String STATUS = "Status";
	private static final String SORT_ORDER = "sortOrder";

	private final ActionConfigRepository actionConfigRepository;
	private final CategoryRepository categoryRepository;
	private final ErrandsRepository errandsRepository;
	private final ExternalIdTypeRepository externalIdTypeRepository;
	private final MeasureTypeRepository measureTypeRepository;
	private final AttachmentPurposeRepository attachmentPurposeRepository;
	private final AttachmentRepository attachmentRepository;
	private final DecisionOutcomeRepository decisionOutcomeRepository;
	private final StatementOutcomeRepository statementOutcomeRepository;
	private final MetadataLabelRepository metadataLabelRepository;
	private final PhaseRepository phaseRepository;
	private final RoleRepository roleRepository;
	private final StatusRepository statusRepository;
	private final ValidationRepository validationRepository;
	private final ContactReasonRepository contactReasonRepository;
	private final JobService jobService;
	private final LabelMoveWorker labelMoveWorker;
	private final AsyncTaskExecutor labelMoveTaskExecutor;
	private final AntPathMatcher pathMatcher;
	private final TransactionTemplate readOnlyTransactionTemplate;
	private final Duration jobStaleAfter;

	public MetadataService(
		final ActionConfigRepository actionConfigRepository,
		final CategoryRepository categoryRepository,
		final ErrandsRepository errandsRepository,
		final ExternalIdTypeRepository externalIdTypeRepository,
		final MeasureTypeRepository measureTypeRepository,
		final AttachmentPurposeRepository attachmentPurposeRepository,
		final AttachmentRepository attachmentRepository,
		final DecisionOutcomeRepository decisionOutcomeRepository,
		final StatementOutcomeRepository statementOutcomeRepository,
		final MetadataLabelRepository metadataLabelRepository,
		final PhaseRepository phaseRepository,
		final RoleRepository roleRepository,
		final StatusRepository statusRepository,
		final ValidationRepository validationRepository,
		final ContactReasonRepository contactReasonRepository,
		final JobService jobService,
		// Lazy: LabelMoveWorker sits behind ErrandService -> RevisionService -> AccessControlService -> AccessMapperService
		// -> MetadataService, a cycle back to this very bean. Never actually needed before the async dispatch fires, by
		// which point every bean in the cycle is already constructed.
		@Lazy final LabelMoveWorker labelMoveWorker,
		@Qualifier("labelMoveTaskExecutor") final AsyncTaskExecutor labelMoveTaskExecutor,
		final PlatformTransactionManager transactionManager,
		final JobProperties jobProperties) {
		this.actionConfigRepository = actionConfigRepository;
		this.categoryRepository = categoryRepository;
		this.errandsRepository = errandsRepository;
		this.externalIdTypeRepository = externalIdTypeRepository;
		this.measureTypeRepository = measureTypeRepository;
		this.attachmentPurposeRepository = attachmentPurposeRepository;
		this.attachmentRepository = attachmentRepository;
		this.decisionOutcomeRepository = decisionOutcomeRepository;
		this.statementOutcomeRepository = statementOutcomeRepository;
		this.metadataLabelRepository = metadataLabelRepository;
		this.phaseRepository = phaseRepository;
		this.roleRepository = roleRepository;
		this.statusRepository = statusRepository;
		this.validationRepository = validationRepository;
		this.contactReasonRepository = contactReasonRepository;
		this.jobService = jobService;
		this.labelMoveWorker = labelMoveWorker;
		this.labelMoveTaskExecutor = labelMoveTaskExecutor;
		this.pathMatcher = new AntPathMatcher();
		this.pathMatcher.setCaseSensitive(false);
		this.readOnlyTransactionTemplate = new TransactionTemplate(transactionManager);
		this.readOnlyTransactionTemplate.setReadOnly(true);
		this.jobStaleAfter = jobProperties.staleAfter();
	}

	// =================================================================
	// Common operations
	// =================================================================

	public MetadataResponse findAll(final String namespace, final String municipalityId) {
		return MetadataResponse.create()
			.withCategories(findCategories(namespace, municipalityId, Sort.unsorted()))
			.withLabels(findLabels(namespace, municipalityId))
			.withStatuses(findStatuses(namespace, municipalityId, Sort.unsorted()))
			.withRoles(findRoles(namespace, municipalityId, Sort.unsorted()))
			.withMeasureTypes(findMeasureTypes(namespace, municipalityId, null, Sort.unsorted()))
			.withAttachmentPurposes(findAttachmentPurposes(namespace, municipalityId, Sort.unsorted()))
			.withDecisionOutcomes(findDecisionOutcomes(namespace, municipalityId, Sort.unsorted()))
			.withStatementOutcomes(findStatementOutcomes(namespace, municipalityId, Sort.unsorted()))
			.withExternalIdTypes(findExternalIdTypes(namespace, municipalityId, Sort.unsorted()))
			.withContactReasons(findContactReasons(namespace, municipalityId, Sort.unsorted()))
			.withPhases(findPhases(namespace, municipalityId));
	}

	public boolean isValidated(final String namespace, final String municipalityId, final EntityType type) {
		return validationRepository.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)
			.map(ValidationEntity::isValidated)
			.orElse(false);
	}

	// =================================================================
	// ExternalIdType operations
	// =================================================================

	public String createExternalIdType(final String namespace, final String municipalityId, final ExternalIdType externalIdType) {
		if (externalIdTypeRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, externalIdType.getName())) {
			throw Problem.valueOf(BAD_REQUEST, ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(EXTERNAL_ID_TYPE, externalIdType.getName(), namespace, municipalityId));
		}

		return externalIdTypeRepository.save(toExternalIdTypeEntity(namespace, municipalityId, externalIdType)).getId();
	}

	public ExternalIdType getExternalIdType(final String namespace, final String municipalityId, final String id) {
		if (!externalIdTypeRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(EXTERNAL_ID_TYPE, id, namespace, municipalityId));
		}

		return toExternalIdType(externalIdTypeRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId));
	}

	public List<ExternalIdType> findExternalIdTypes(final String namespace, final String municipalityId, final Sort sort) {
		return externalIdTypeRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, getDefaultSortIfUnsorted(sort))
			.stream()
			.map(MetadataMapper::toExternalIdType)
			.toList();
	}

	public void deleteExternalIdType(final String namespace, final String municipalityId, final String id) {
		if (!externalIdTypeRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(EXTERNAL_ID_TYPE, id, namespace, municipalityId));
		}

		externalIdTypeRepository.deleteByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
	}

	public ExternalIdType updateExternalIdType(final String namespace, final String municipalityId, final String id, final ExternalIdType externalIdType) {
		if (!externalIdTypeRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(EXTERNAL_ID_TYPE, id, namespace, municipalityId));
		}
		final var entity = updateExternalIdTypeEntity(externalIdTypeRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId), externalIdType);
		return toExternalIdType(externalIdTypeRepository.save(entity));
	}

	// =================================================================
	// Status operations
	// =================================================================

	public String createStatus(final String namespace, final String municipalityId, final Status status) {
		if (statusRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, status.getName())) {
			throw Problem.valueOf(BAD_REQUEST, ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(STATUS, status.getName(), namespace, municipalityId));
		}

		return statusRepository.save(toStatusEntity(namespace, municipalityId, status)).getId();
	}

	public Status getStatus(final String namespace, final String municipalityId, final String id) {
		if (!statusRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(STATUS, id, namespace, municipalityId));
		}

		return toStatus(statusRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId));
	}

	public List<Status> findStatuses(final String namespace, final String municipalityId, final Sort sort) {
		return statusRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, getDefaultSortIfUnsorted(sort))
			.stream()
			.map(MetadataMapper::toStatus)
			.toList();
	}

	public void deleteStatus(final String namespace, final String municipalityId, final String id) {
		if (!statusRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(STATUS, id, namespace, municipalityId));
		}

		statusRepository.deleteByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
	}

	public Status updateStatus(final String namespace, final String municipalityId, final String id, final Status status) {
		if (!statusRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(STATUS, id, namespace, municipalityId));
		}
		final var entity = updateStatusEntity(statusRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId), status);
		return toStatus(statusRepository.save(entity));
	}

	// =================================================================
	// Role operations
	// =================================================================

	public String createRole(final String namespace, final String municipalityId, final Role role) {
		if (roleRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, role.getName())) {
			throw Problem.valueOf(BAD_REQUEST, ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(ROLE, role.getName(), namespace, municipalityId));
		}

		return roleRepository.save(toRoleEntity(namespace, municipalityId, role)).getId();
	}

	public Role getRole(final String namespace, final String municipalityId, final String id) {
		if (!roleRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(ROLE, id, namespace, municipalityId));
		}

		return toRole(roleRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId));
	}

	public List<Role> findRoles(final String namespace, final String municipalityId, final Sort sort) {
		return roleRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, getDefaultSortIfUnsorted(sort))
			.stream()
			.map(MetadataMapper::toRole)
			.toList();
	}

	public void deleteRole(final String namespace, final String municipalityId, final String id) {
		if (!roleRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(ROLE, id, namespace, municipalityId));
		}

		roleRepository.deleteByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
	}

	public Role updateRole(final String namespace, final String municipalityId, final String id, final Role role) {
		if (!roleRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(ROLE, id, namespace, municipalityId));
		}
		final var entity = updateRoleEntity(roleRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId), role);
		return toRole(roleRepository.save(entity));
	}

	// =================================================================
	// Label operations
	// =================================================================

	public void createLabels(final String namespace, final String municipalityId, final List<Label> labels) {
		metadataLabelRepository.saveAll(toMetadataLabelEntityList(namespace, municipalityId, labels));
	}

	@Transactional
	public void updateLabels(final String namespace, final String municipalityId, final List<Label> labels) {
		// Fetch all existing labels and verify existence
		final var allExisting = metadataLabelRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId);
		if (allExisting.isEmpty()) {
			throw Problem.valueOf(NOT_FOUND, "Labels are not present in namespace '%s' for municipalityId '%s'".formatted(namespace, municipalityId));
		}

		// Determine which labels are being removed
		final var existingIds = allExisting.stream()
			.map(MetadataLabelEntity::getId)
			.collect(Collectors.toSet());
		final var incomingIds = collectLabelIds(labels);
		final var removedIds = existingIds.stream()
			.filter(id -> !incomingIds.contains(id))
			.collect(Collectors.toSet());

		// Verify no removed labels are referenced by errands
		if (!removedIds.isEmpty() && errandsRepository.existsByLabelsMetadataLabelIdIn(removedIds)) {
			throw Problem.valueOf(BAD_REQUEST, "Cannot delete labels with ids %s because they are referenced by one or more errands".formatted(removedIds));
		}

		// Delete removed root labels (cascade handles their children)
		final var existingRoots = metadataLabelRepository.findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId);
		final var removedRoots = existingRoots.stream()
			.filter(root -> removedIds.contains(root.getId()))
			.toList();
		removedRoots.forEach(root -> metadataLabelRepository.deleteById(root.getId()));
		metadataLabelRepository.flush();

		// Merge remaining roots with incoming data (orphanRemoval handles child deletion)
		final var remainingRoots = new ArrayList<>(existingRoots);
		remainingRoots.removeAll(removedRoots);
		updateMetadataLabelEntities(remainingRoots, labels, namespace, municipalityId);
		metadataLabelRepository.saveAll(remainingRoots);
	}

	private static Set<String> collectLabelIds(final List<Label> labels) {
		if (isEmpty(labels)) {
			return Set.of();
		}
		return labels.stream()
			.flatMap(label -> Stream.concat(
				Stream.ofNullable(label.getId()),
				collectLabelIds(label.getLabels()).stream()))
			.collect(Collectors.toSet());
	}

	public Labels findLabels(final String namespace, final String municipalityId) {
		return toLabels(metadataLabelRepository.findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId));
	}

	public boolean labelExistsById(final String id, final String namespace, final String municipalityId) {
		return metadataLabelRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
	}

	@Transactional(readOnly = true)
	public LabelMoveDryRunResponse moveLabel(final String namespace, final String municipalityId, final String labelId, final LabelMoveRequest request) {
		var context = validateAndFindLabelToMove(namespace, municipalityId, labelId, request.getNewParentId());
		var allMovedIds = collectMovedLabelIds(context.labelToMove().getId(), context.descendants());

		var affectedErrandIds = errandsRepository.findDistinctIdsByLabelsMetadataLabelIdIn(allMovedIds);
		var affectedActions = resolveAffectedActions(namespace, municipalityId, allMovedIds);

		return LabelMoveDryRunResponse.create()
			.withAffectedErrandCount(affectedErrandIds.size())
			.withAffectedActions(affectedActions);
	}

	/**
	 * The actions a move of these label ids would affect - shared by the dry-run response and, so an admin who skips
	 * straight to a real move learns the same thing, the {@link JobResponse} {@link #startLabelMove} returns.
	 */
	private List<AffectedAction> resolveAffectedActions(final String namespace, final String municipalityId, final Set<String> movedLabelIds) {
		return actionConfigRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId).stream()
			.filter(action -> isAffectedByMove(action, movedLabelIds))
			.map(action -> AffectedAction.create()
				.withId(action.getId())
				.withName(action.getName())
				.withDisplayValue(action.getDisplayValue()))
			.toList();
	}

	/**
	 * Starts a label move as an asynchronous job, reported through {@code GET .../jobs/{jobId}}.
	 * <p>
	 * Refused if another job is genuinely still under way for the namespace, not just for this label — two moves in
	 * the same namespace can target overlapping subtrees (one label and one of its own descendants, or a label and the
	 * destination it is headed into) without either id matching the other, so a check scoped to this label alone would
	 * let them run at the same time and race on the same errands. Namespace-wide serialization costs nothing here,
	 * since label moves are rare.
	 * <p>
	 * "Genuinely" matters: {@link JobService#stealStaleLease} is asked rather than {@link JobService#hasActiveJob}, so
	 * a job whose instance died mid-run does not go on blocking the namespace for as long as
	 * {@link JobProperties#staleAfter()} allows - only until the next caller tries to start a move, at which point the
	 * stale lease is reclaimed on the spot. This does not by itself give a crashed run a way to resume the restow it
	 * left half done; it only shrinks how long the namespace stays blocked because of it.
	 * <p>
	 * Deliberately not itself {@code @Transactional}, and validation, job creation and dispatch are kept in three
	 * separate steps rather than one enclosing transaction — mirrors {@link ErrandPurgeService#startPurge}. Wrapping
	 * the whole method would flush {@link JobService#create}'s row without committing it before the worker is handed
	 * to the executor, and the worker's {@link JobService#setRunning} runs in its own {@code REQUIRES_NEW} transaction
	 * on a different thread that cannot see an uncommitted row — it would find no job, log a warning, and leave the
	 * job stuck PENDING until the stale-job sweep eventually fails it. Validation still needs a session of its own:
	 * {@link #validateAndFindLabelToMove}'s cycle check walks LAZY {@code parent} proxies one hop at a time, and each
	 * hop past the first needs the session to still be there to load from - hence {@link #readOnlyTransactionTemplate}
	 * rather than a plain call, which would only cover the very first repository call before the session behind it
	 * closes.
	 */
	public JobResponse startLabelMove(final String namespace, final String municipalityId, final String labelId, final LabelMoveRequest request) {
		var context = readOnlyTransactionTemplate.execute(status -> validateAndFindLabelToMove(namespace, municipalityId, labelId, request.getNewParentId()));
		var canonicalLabelId = context.labelToMove().getId();

		if (!jobService.stealStaleLease(namespace, municipalityId, MOVE_LABEL, jobStaleAfter)) {
			throw Problem.valueOf(CONFLICT, MOVE_ALREADY_IN_PROGRESS.formatted(namespace, municipalityId));
		}

		var allMovedIds = collectMovedLabelIds(canonicalLabelId, context.descendants());
		// Resolved once, here - the job's own total and the walk that restows them both read from this exact list
		// rather than each re-deriving their own (see LabelMoveRun#errandIds for why that matters). Frozen at this
		// point deliberately: an errand created after this is created against the tree the move already left in
		// place, so it needs no restowing.
		var affectedErrandIds = errandsRepository.findDistinctIdsByLabelsMetadataLabelIdIn(allMovedIds);
		// Resolved once, here, and attached below to the response this method itself returns - an admin who skips the
		// dry run and starts the move directly still learns which actions are affected, without waiting on a later
		// GET .../jobs/{jobId} that stores no such thing.
		var affectedActions = resolveAffectedActions(namespace, municipalityId, allMovedIds);
		// Committed by the time this call returns, since it is not wrapped in a transaction of this method's own - the
		// worker dispatched right after is free to look the job up from another thread.
		var jobId = jobService.create(namespace, municipalityId, MOVE_LABEL, affectedErrandIds.size(), canonicalLabelId);
		var startedBy = startedBy();

		try {
			labelMoveTaskExecutor.execute(() -> labelMoveWorker.run(new LabelMoveRun(jobId, namespace, municipalityId, canonicalLabelId, request.getNewParentId(), affectedErrandIds, startedBy)));
		} catch (final Exception e) {
			// The job is already there and would otherwise sit waiting for a run that never comes.
			jobService.fail(jobId, COULD_NOT_START.formatted(e.getMessage()));

			throw e instanceof final ThrowableProblem problem ? problem : Problem.valueOf(INTERNAL_SERVER_ERROR, COULD_NOT_START.formatted(e.getMessage()));
		}

		return jobService.get(namespace, municipalityId, jobId).withAffectedActions(affectedActions);
	}

	/**
	 * The caller a label move is recorded against. Read here, on the request thread, since the thread carrying out the
	 * run has no identifier of its own to read.
	 * <p>
	 * Carries the whole identifier - type and value, via {@link Identifier#toHeaderValue()} - rather than just the
	 * value, so that {@link EventService#createLabelMoveEvent} can rebuild the original {@link Identifier} instead of
	 * defaulting the type to {@code CUSTOM} and misrepresenting an AD user's account name as a party id in the audit
	 * trail.
	 */
	private static String startedBy() {
		return ofNullable(Identifier.get())
			.map(Identifier::toHeaderValue)
			.orElse(UNKNOWN_CALLER);
	}

	/**
	 * The moved label together with every descendant under it, so that an errand tagged with any label in the subtree
	 * counts as affected — regardless of whether the ancestor-chain invariant every errand is meant to carry has actually
	 * caught up with it yet.
	 */
	private static Set<String> collectMovedLabelIds(final String labelId, final List<MetadataLabelEntity> descendants) {
		var ids = new HashSet<String>();
		ids.add(labelId);
		descendants.forEach(descendant -> ids.add(descendant.getId()));
		return ids;
	}

	/**
	 * The moved label plus the descendants that move with it — read once by {@link #validateAndFindLabelToMove} and
	 * reused by both callers, so that neither {@link #moveLabel} nor {@link #startLabelMove} re-reads the descendant
	 * tree that validation already fetched.
	 */
	private record LabelMoveContext(MetadataLabelEntity labelToMove, List<MetadataLabelEntity> descendants) {
	}

	private LabelMoveContext validateAndFindLabelToMove(final String namespace, final String municipalityId, final String labelId, final String newParentId) {
		var labelToMove = metadataLabelRepository.findByIdAndNamespaceAndMunicipalityId(labelId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(LABEL, labelId, namespace, municipalityId)));

		MetadataLabelEntity newParent = null;
		if (newParentId != null) {
			newParent = metadataLabelRepository.findByIdAndNamespaceAndMunicipalityId(newParentId, namespace, municipalityId)
				.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(LABEL, newParentId, namespace, municipalityId)));
		}

		validateNotNoOp(labelToMove, newParentId);
		validateNoCycle(labelToMove.getId(), newParent);

		var newPath = newParent != null
			? newParent.getResourcePath() + "/" + labelToMove.getResourceName()
			: labelToMove.getResourceName();

		validatePathNotTaken(namespace, municipalityId, labelToMove.getId(), newPath);

		var descendants = metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(
			namespace, municipalityId, labelToMove.getResourcePath() + "/");

		validateNoDescendantPathCollision(namespace, municipalityId, labelToMove, newPath, descendants);
		validateResourcePathLength(labelToMove, newPath, descendants);

		return new LabelMoveContext(labelToMove, descendants);
	}

	private static void validateNotNoOp(final MetadataLabelEntity labelToMove, final String newParentId) {
		var currentParentId = labelToMove.getParent() != null ? labelToMove.getParent().getId() : null;
		if (Objects.equals(currentParentId, newParentId)) {
			throw Problem.valueOf(BAD_REQUEST, "Label '%s' is already under the specified parent — move would be a no-op".formatted(labelToMove.getId()));
		}
	}

	/**
	 * {@code labelId} must be the moved label's id as stored, not the raw path variable — a client sending the same
	 * UUID in a different case would otherwise never match {@code current.getId()} on the way up, since both sides
	 * of the comparison have to come from the same, canonical source to line up.
	 */
	private static void validateNoCycle(final String labelId, final MetadataLabelEntity newParent) {
		if (newParent == null) {
			return;
		}
		var visited = new HashSet<String>();
		var current = newParent;
		while (current != null) {
			if (!visited.add(current.getId())) {
				break;
			}
			if (Objects.equals(labelId, current.getId())) {
				throw Problem.valueOf(BAD_REQUEST, "Moving label '%s' under '%s' would create a cycle".formatted(labelId, newParent.getId()));
			}
			current = current.getParent();
		}
	}

	private void validatePathNotTaken(final String namespace, final String municipalityId, final String movingLabelId, final String candidatePath) {
		metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePath(namespace, municipalityId, candidatePath)
			.filter(existing -> !Objects.equals(existing.getId(), movingLabelId))
			.ifPresent(existing -> {
				throw Problem.valueOf(CONFLICT, "A label with path '%s' already exists under the destination".formatted(candidatePath));
			});
	}

	/**
	 * A descendant's resulting path can collide just as easily as the moved label's own, since both land under a
	 * destination neither of them has occupied before — checked here, once the subtree {@link #validateResourcePathLength}
	 * also needs has been read, rather than folded into the moved label's own check above.
	 */
	private void validateNoDescendantPathCollision(final String namespace, final String municipalityId, final MetadataLabelEntity labelToMove, final String newPath, final List<MetadataLabelEntity> descendants) {
		var oldPrefixLength = labelToMove.getResourcePath().length();
		descendants.forEach(descendant -> validatePathNotTaken(namespace, municipalityId, descendant.getId(), newPath + descendant.getResourcePath().substring(oldPrefixLength)));
	}

	/**
	 * The moved label's new path, and the new path every descendant it carries along would get, must each fit the
	 * resource_path column — rejected here, before any row is touched, rather than surfacing as a database error
	 * partway through the restructuring.
	 */
	private static void validateResourcePathLength(final MetadataLabelEntity labelToMove, final String newPath, final List<MetadataLabelEntity> descendants) {
		rejectIfTooLong(newPath);

		var oldPrefixLength = labelToMove.getResourcePath().length();
		descendants.forEach(descendant -> rejectIfTooLong(newPath + descendant.getResourcePath().substring(oldPrefixLength)));
	}

	private static void rejectIfTooLong(final String resourcePath) {
		if (resourcePath.length() > RESOURCE_PATH_MAX_LENGTH) {
			throw Problem.valueOf(BAD_REQUEST,
				"Resulting resource path '%s' (%d characters) exceeds the maximum of %d characters".formatted(resourcePath, resourcePath.length(), RESOURCE_PATH_MAX_LENGTH));
		}
	}

	private static boolean isAffectedByMove(final ActionConfigEntity action, final Set<String> movedLabelIds) {
		return action.getConditions().stream()
			.filter(c -> HAS_LABEL.equals(c.getKey()))
			.flatMap(c -> c.getValues().stream())
			.anyMatch(movedLabelIds::contains);
	}

	public boolean hasLabels(final String namespace, final String municipalityId) {
		return metadataLabelRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

	public void deleteLabels(final String namespace, final String municipalityId) {
		if (!metadataLabelRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, "Labels are not present in namespace '%s' for municipalityId '%s'".formatted(namespace, municipalityId));
		}

		metadataLabelRepository.findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId).stream()
			.map(MetadataLabelEntity::getId)
			.forEach(metadataLabelRepository::deleteById);
	}

	/**
	 * Resolves the labels of the namespace matching each group of resource path patterns.
	 * <p>
	 * The labels are read once for every group rather than once per group, so that the groups are answered from a single
	 * state of the label table and a caller resolving several of them pays one read.
	 *
	 * @param  namespace            namespace
	 * @param  municipalityId       municipality id
	 * @param  resourcePathPatterns patterns to match, per group
	 * @return                      labels matching the patterns of each group, empty for a group carrying no patterns
	 */
	@Transactional(readOnly = true)
	public <K> Map<K, Set<MetadataLabelEntity>> patternToLabels(
		final String namespace,
		final String municipalityId,
		final Map<K, List<String>> resourcePathPatterns) {

		if (resourcePathPatterns.values().stream().allMatch(CollectionUtils::isEmpty)) {
			return resourcePathPatterns.keySet().stream().collect(Collectors.toMap(key -> key, _ -> Set.of()));
		}

		final var potentialMatches = metadataLabelRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId);

		return resourcePathPatterns.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> matching(potentialMatches, entry.getValue())));
	}

	private Set<MetadataLabelEntity> matching(final List<MetadataLabelEntity> potentialMatches, final List<String> resourcePathPatterns) {
		if (isEmpty(resourcePathPatterns)) {
			return Set.of();
		}

		return potentialMatches.stream()
			.filter(entity -> entity.getResourcePath() != null)
			.filter(entity -> resourcePathPatterns.stream()
				.anyMatch(pattern -> pathMatcher.match(pattern, entity.getResourcePath())))
			.collect(Collectors.toSet());
	}

	// =================================================================
	// Category and Type operations
	// =================================================================

	public String createCategory(final String namespace, final String municipalityId, final Category category) {
		if (categoryRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, category.getName())) {
			throw Problem.valueOf(BAD_REQUEST, ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(CATEGORY, category.getName(), namespace, municipalityId));
		}

		return categoryRepository.save(toCategoryEntity(namespace, municipalityId, category)).getId();
	}

	public Category getCategory(final String namespace, final String municipalityId, final String id) {
		if (!categoryRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(CATEGORY, id, namespace, municipalityId));
		}

		return MetadataMapper.toCategory(categoryRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId));
	}

	public Category updateCategory(final String namespace, final String municipalityId, final String id, final Category category) {
		if (!categoryRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(CATEGORY, id, namespace, municipalityId));
		}
		final var entity = updateEntity(categoryRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId), category);
		return toCategory(categoryRepository.save(entity));
	}

	public List<Category> findCategories(final String namespace, final String municipalityId, final Sort sort) {
		return categoryRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, getDefaultSortIfUnsorted(sort))
			.stream()
			.map(MetadataMapper::toCategory)
			.toList();
	}

	public List<Type> findTypes(final String namespace, final String municipalityId, final String category) {
		return findCategories(namespace, municipalityId, Sort.unsorted())
			.stream()
			.filter(entry -> Objects.equals(category, entry.getName()))
			.map(Category::getTypes)
			.findAny()
			.orElse(emptyList())
			.stream()
			.sorted(comparing(Type::getDisplayName, nullsFirst(naturalOrder())))
			.toList();
	}

	public List<Type> findTypesByCategoryId(final String namespace, final String municipalityId, final String id) {
		if (!categoryRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(CATEGORY, id, namespace, municipalityId));
		}

		return ofNullable(toCategory(categoryRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)).getTypes())
			.orElse(emptyList())
			.stream()
			.sorted(comparing(Type::getDisplayName, nullsFirst(naturalOrder())))
			.toList();
	}

	public void deleteCategory(final String namespace, final String municipalityId, final String id) {
		if (!categoryRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(CATEGORY, id, namespace, municipalityId));
		}

		categoryRepository.deleteByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
	}

	// =================================================================
	// ContactReason Operations
	// =================================================================

	public List<ContactReason> findContactReasons(final String namespace, final String municipalityId, final Sort sort) {
		return contactReasonRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, getDefaultSortIfUnsorted(sort)).stream()
			.map(MetadataMapper::toContactReason)
			.toList();
	}

	public String createContactReason(final String namespace, final String municipalityId, final ContactReason contactReason) {
		return contactReasonRepository.save(toContactReasonEntity(namespace, municipalityId, contactReason)).getId();
	}

	public ContactReason getContactReasonByIdAndNamespaceAndMunicipalityId(final String contactReasonId, final String namespace, final String municipalityId) {
		final var contactReasonEntity = contactReasonRepository.findByIdAndNamespaceAndMunicipalityId(contactReasonId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(CONTACT_REASON, contactReasonId, namespace, municipalityId)));
		return toContactReason(contactReasonEntity);
	}

	public ContactReason patchContactReason(final String contactReasonId, final String namespace, final String municipalityId, final ContactReason contactReason) {
		if (!contactReasonRepository.existsByIdAndNamespaceAndMunicipalityId(contactReasonId, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(CONTACT_REASON, contactReasonId, namespace, municipalityId));
		}
		final var contactReasonEntity = updateContactReason(contactReasonRepository.getByIdAndNamespaceAndMunicipalityId(contactReasonId, namespace, municipalityId), contactReason);

		return toContactReason(contactReasonRepository.save(contactReasonEntity));
	}

	@Transactional
	public void deleteContactReason(final String contactReasonId, final String namespace, final String municipalityId) {
		if (!contactReasonRepository.existsByIdAndNamespaceAndMunicipalityId(contactReasonId, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(CONTACT_REASON, contactReasonId, namespace, municipalityId));
		}
		contactReasonRepository.deleteByIdAndNamespaceAndMunicipalityId(contactReasonId, namespace, municipalityId);
	}

	// =================================================================
	// Phase operations
	// =================================================================

	@Transactional
	public String createPhase(final String namespace, final String municipalityId, final Phase phase) {
		if (phaseRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, phase.getName())) {
			throw Problem.valueOf(BAD_REQUEST, ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(PHASE, phase.getName(), namespace, municipalityId));
		}

		return phaseRepository.save(toPhaseEntity(namespace, municipalityId, phase)).getId();
	}

	public Phase getPhase(final String namespace, final String municipalityId, final String phaseId) {
		return phaseRepository.findByIdAndNamespaceAndMunicipalityId(phaseId, namespace, municipalityId)
			.map(entity -> {
				final var phase = toPhase(entity);
				enrichPhaseTransitions(phase, namespace, municipalityId);
				return phase;
			})
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(PHASE, phaseId, namespace, municipalityId)));
	}

	public List<Phase> findPhases(final String namespace, final String municipalityId) {
		return phaseRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId).stream()
			.map(MetadataMapper::toPhase)
			.map(phase -> {
				enrichPhaseTransitions(phase, namespace, municipalityId);
				return phase;
			})
			.sorted(comparing(Phase::getPhaseOrder, nullsFirst(naturalOrder())))
			.toList();
	}

	@Transactional
	public Phase patchPhase(final String phaseId, final String namespace, final String municipalityId, final Phase phase) {
		final var entity = phaseRepository.findByIdAndNamespaceAndMunicipalityId(phaseId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(PHASE, phaseId, namespace, municipalityId)));

		final var updatedPhase = toPhase(phaseRepository.save(updatePhaseEntity(entity, phase)));
		enrichPhaseTransitions(updatedPhase, namespace, municipalityId);
		return updatedPhase;
	}

	@Transactional
	public void deletePhase(final String phaseId, final String namespace, final String municipalityId) {
		if (!phaseRepository.existsByIdAndNamespaceAndMunicipalityId(phaseId, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(PHASE, phaseId, namespace, municipalityId));
		}

		if (errandsRepository.existsByPhasesPhaseEntityId(phaseId)) {
			throw Problem.valueOf(BAD_REQUEST, "Phase '%s' cannot be deleted because it is referenced by one or more errands".formatted(phaseId));
		}

		phaseRepository.deleteByIdAndNamespaceAndMunicipalityId(phaseId, namespace, municipalityId);
	}

	private void enrichPhaseTransitions(final Phase phase, final String namespace, final String municipalityId) {
		ofNullable(phase.getTransitions()).orElse(emptyList()).forEach(transition -> {
			phaseRepository.findByIdAndNamespaceAndMunicipalityId(transition.getTargetPhaseId(), namespace, municipalityId)
				.ifPresent(targetPhase -> {
					transition.setTargetPhaseName(targetPhase.getName());
					transition.setTargetPhaseDisplayName(targetPhase.getDisplayName());
				});
		});
	}

	// =================================================================
	// Phase Transition operations
	// =================================================================

	@Transactional
	public String createPhaseTransition(final String namespace, final String municipalityId, final String phaseId, final PhaseTransition transition) {
		final var phaseEntity = phaseRepository.findByIdAndNamespaceAndMunicipalityId(phaseId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(PHASE, phaseId, namespace, municipalityId)));

		if (!phaseRepository.existsByIdAndNamespaceAndMunicipalityId(transition.getTargetPhaseId(), namespace, municipalityId)) {
			throw Problem.valueOf(BAD_REQUEST, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(PHASE, transition.getTargetPhaseId(), namespace, municipalityId));
		}

		final var transitionEntity = toPhaseTransitionEntity(phaseEntity, transition);
		phaseEntity.getTransitions().add(transitionEntity);
		phaseRepository.save(phaseEntity);

		return transitionEntity.getId();
	}

	public List<PhaseTransition> findPhaseTransitions(final String namespace, final String municipalityId, final String phaseId) {
		final var phaseEntity = phaseRepository.findByIdAndNamespaceAndMunicipalityId(phaseId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(PHASE, phaseId, namespace, municipalityId)));

		return phaseEntity.getTransitions().stream()
			.map(MetadataMapper::toPhaseTransition)
			.map(transition -> {
				phaseRepository.findByIdAndNamespaceAndMunicipalityId(transition.getTargetPhaseId(), namespace, municipalityId)
					.ifPresent(targetPhase -> {
						transition.setTargetPhaseName(targetPhase.getName());
						transition.setTargetPhaseDisplayName(targetPhase.getDisplayName());
					});
				return transition;
			})
			.toList();
	}

	@Transactional
	public void deletePhaseTransition(final String namespace, final String municipalityId, final String phaseId, final String transitionId) {
		final var phaseEntity = phaseRepository.findByIdAndNamespaceAndMunicipalityId(phaseId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(PHASE, phaseId, namespace, municipalityId)));

		final var removed = phaseEntity.getTransitions().removeIf(t -> Objects.equals(t.getId(), transitionId));
		if (!removed) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(PHASE_TRANSITION, transitionId, namespace, municipalityId));
		}

		phaseRepository.save(phaseEntity);
	}

	// =================================================================
	// MeasureType operations
	// =================================================================

	/**
	 * A measure type belongs to at least one group - a type in no group cannot be found by the one thing measure types
	 * are looked up by.
	 * <p>
	 * Held here rather than on the model, which the update shares: a constraint there would demand the groups of every
	 * patch, where every other property of a measure type may be left out.
	 */
	private static void verifyMeasureGroupsNamed(final List<String> measureGroups) {
		if (isEmpty(measureGroups)) {
			throw Problem.valueOf(BAD_REQUEST, MEASURE_TYPE_WITHOUT_GROUP);
		}
	}

	/** An update may leave the groups out, which changes nothing, but may not empty them. */
	private static void verifyMeasureGroupsNotEmptied(final List<String> measureGroups) {
		if (nonNull(measureGroups)) {
			verifyMeasureGroupsNamed(measureGroups);
		}
	}

	public String createMeasureType(final String namespace, final String municipalityId, final MeasureType measureType) {
		verifyMeasureGroupsNamed(measureType.getMeasureGroups());
		if (measureTypeRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, measureType.getName())) {
			throw Problem.valueOf(BAD_REQUEST, ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(MEASURE_TYPE, measureType.getName(), namespace, municipalityId));
		}

		return measureTypeRepository.save(toMeasureTypeEntity(namespace, municipalityId, measureType)).getId();
	}

	public MeasureType getMeasureType(final String namespace, final String municipalityId, final String id) {
		if (!measureTypeRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(MEASURE_TYPE, id, namespace, municipalityId));
		}

		return toMeasureType(measureTypeRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId));
	}

	public List<MeasureType> findMeasureTypes(final String namespace, final String municipalityId, final String measureGroup, final Sort sort) {
		final var sortToUse = getDefaultSortIfUnsorted(sort);
		verifySortable(sortToUse);

		return ofNullable(measureGroup)
			.map(group -> measureTypeRepository.findAllByNamespaceAndMunicipalityIdAndMeasureGroupsContaining(namespace, municipalityId, group, sortToUse))
			.orElseGet(() -> measureTypeRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, sortToUse))
			.stream()
			.map(MetadataMapper::toMeasureType)
			.toList();
	}

	public void deleteMeasureType(final String namespace, final String municipalityId, final String id) {
		if (!measureTypeRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(MEASURE_TYPE, id, namespace, municipalityId));
		}

		measureTypeRepository.deleteByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
	}

	public MeasureType updateMeasureType(final String namespace, final String municipalityId, final String id, final MeasureType measureType) {
		verifyMeasureGroupsNotEmptied(measureType.getMeasureGroups());
		if (!measureTypeRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(MEASURE_TYPE, id, namespace, municipalityId));
		}
		final var entity = updateMeasureTypeEntity(measureTypeRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId), measureType);
		return toMeasureType(measureTypeRepository.save(entity));
	}

	/**
	 * The properties a measure type may be sorted by, which is every scalar it carries.
	 * <p>
	 * The groups became a collection, and a collection cannot be sorted on. A request naming one sorted a measure type
	 * before that and would otherwise reach the query derivation as a property that is not there, which answers 500
	 * without saying what is wrong.
	 */
	private static final Set<String> SORTABLE_MEASURE_TYPE_PROPERTIES = Arrays.stream(MeasureTypeEntity.class.getDeclaredFields())
		.filter(field -> !field.isSynthetic())
		.filter(field -> !Modifier.isStatic(field.getModifiers()))
		.filter(field -> !Collection.class.isAssignableFrom(field.getType()))
		.map(Field::getName)
		.collect(toSet());

	private static void verifySortable(final Sort sort) {
		sort.forEach(order -> {
			if (!SORTABLE_MEASURE_TYPE_PROPERTIES.contains(order.getProperty())) {
				throw Problem.valueOf(BAD_REQUEST, "'%s' is not a property a measure type can be sorted by".formatted(order.getProperty()));
			}
		});
	}

	private Sort getDefaultSortIfUnsorted(final Sort sort) {
		return (Objects.isNull(sort) || sort.isUnsorted()) ? Sort.by(SORT_ORDER) : sort;
	}

	// =================================================================
	// AttachmentPurpose operations
	// =================================================================

	public String createAttachmentPurpose(final String namespace, final String municipalityId, final AttachmentPurpose attachmentPurpose) {
		if (attachmentPurposeRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, attachmentPurpose.getName())) {
			throw Problem.valueOf(BAD_REQUEST, ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(ATTACHMENT_PURPOSE, attachmentPurpose.getName(), namespace, municipalityId));
		}

		return attachmentPurposeRepository.save(toAttachmentPurposeEntity(namespace, municipalityId, attachmentPurpose)).getId();
	}

	public AttachmentPurpose getAttachmentPurpose(final String namespace, final String municipalityId, final String id) {
		if (!attachmentPurposeRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(ATTACHMENT_PURPOSE, id, namespace, municipalityId));
		}

		return toAttachmentPurpose(attachmentPurposeRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId));
	}

	public List<AttachmentPurpose> findAttachmentPurposes(final String namespace, final String municipalityId, final Sort sort) {
		return attachmentPurposeRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, getDefaultSortIfUnsorted(sort))
			.stream()
			.map(MetadataMapper::toAttachmentPurpose)
			.toList();
	}

	public void deleteAttachmentPurpose(final String namespace, final String municipalityId, final String id) {
		if (!attachmentPurposeRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(ATTACHMENT_PURPOSE, id, namespace, municipalityId));
		}

		if (attachmentRepository.existsByPurposeId(id)) {
			throw Problem.valueOf(BAD_REQUEST, ATTACHMENT_PURPOSE_IN_USE.formatted(id));
		}

		attachmentPurposeRepository.deleteByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
	}

	public AttachmentPurpose updateAttachmentPurpose(final String namespace, final String municipalityId, final String id, final AttachmentPurpose attachmentPurpose) {
		if (!attachmentPurposeRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(ATTACHMENT_PURPOSE, id, namespace, municipalityId));
		}
		if ((attachmentPurpose.getName() != null) && attachmentPurposeRepository.existsByNamespaceAndMunicipalityIdAndNameAndIdNot(namespace, municipalityId, attachmentPurpose.getName(), id)) {
			throw Problem.valueOf(BAD_REQUEST, ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(ATTACHMENT_PURPOSE, attachmentPurpose.getName(), namespace, municipalityId));
		}
		final var entity = updateAttachmentPurposeEntity(attachmentPurposeRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId), attachmentPurpose);
		return toAttachmentPurpose(attachmentPurposeRepository.save(entity));
	}

	// =================================================================
	// DecisionOutcome operations
	// =================================================================

	public String createDecisionOutcome(final String namespace, final String municipalityId, final DecisionOutcome decisionOutcome) {
		if (decisionOutcomeRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, decisionOutcome.getName())) {
			throw Problem.valueOf(BAD_REQUEST, ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(DECISION_OUTCOME, decisionOutcome.getName(), namespace, municipalityId));
		}

		return decisionOutcomeRepository.save(toDecisionOutcomeEntity(namespace, municipalityId, decisionOutcome)).getId();
	}

	public DecisionOutcome getDecisionOutcome(final String namespace, final String municipalityId, final String id) {
		if (!decisionOutcomeRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(DECISION_OUTCOME, id, namespace, municipalityId));
		}

		return toDecisionOutcome(decisionOutcomeRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId));
	}

	public List<DecisionOutcome> findDecisionOutcomes(final String namespace, final String municipalityId, final Sort sort) {
		return decisionOutcomeRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, getDefaultSortIfUnsorted(sort))
			.stream()
			.map(MetadataMapper::toDecisionOutcome)
			.toList();
	}

	/**
	 * Removes the outcome from what may be given from now on. The decisions and recommendations already given it keep it,
	 * the way an errand keeps a status that has been removed.
	 */
	public void deleteDecisionOutcome(final String namespace, final String municipalityId, final String id) {
		if (!decisionOutcomeRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(DECISION_OUTCOME, id, namespace, municipalityId));
		}

		decisionOutcomeRepository.deleteByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
	}

	public DecisionOutcome updateDecisionOutcome(final String namespace, final String municipalityId, final String id, final DecisionOutcome decisionOutcome) {
		if (!decisionOutcomeRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(DECISION_OUTCOME, id, namespace, municipalityId));
		}
		if ((decisionOutcome.getName() != null) && decisionOutcomeRepository.existsByNamespaceAndMunicipalityIdAndNameAndIdNot(namespace, municipalityId, decisionOutcome.getName(), id)) {
			throw Problem.valueOf(BAD_REQUEST, ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(DECISION_OUTCOME, decisionOutcome.getName(), namespace, municipalityId));
		}
		final var entity = updateDecisionOutcomeEntity(decisionOutcomeRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId), decisionOutcome);
		return toDecisionOutcome(decisionOutcomeRepository.save(entity));
	}

	// =================================================================
	// StatementOutcome operations
	// =================================================================

	public String createStatementOutcome(final String namespace, final String municipalityId, final StatementOutcome statementOutcome) {
		if (statementOutcomeRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, statementOutcome.getName())) {
			throw Problem.valueOf(BAD_REQUEST, ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(STATEMENT_OUTCOME, statementOutcome.getName(), namespace, municipalityId));
		}

		return statementOutcomeRepository.save(toStatementOutcomeEntity(namespace, municipalityId, statementOutcome)).getId();
	}

	public StatementOutcome getStatementOutcome(final String namespace, final String municipalityId, final String id) {
		if (!statementOutcomeRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(STATEMENT_OUTCOME, id, namespace, municipalityId));
		}

		return toStatementOutcome(statementOutcomeRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId));
	}

	public List<StatementOutcome> findStatementOutcomes(final String namespace, final String municipalityId, final Sort sort) {
		return statementOutcomeRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, getDefaultSortIfUnsorted(sort))
			.stream()
			.map(MetadataMapper::toStatementOutcome)
			.toList();
	}

	/**
	 * Removes the outcome from what may be given from now on. The statements already given it keep it, the way an errand
	 * keeps a status that has been removed.
	 */
	public void deleteStatementOutcome(final String namespace, final String municipalityId, final String id) {
		if (!statementOutcomeRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(STATEMENT_OUTCOME, id, namespace, municipalityId));
		}

		statementOutcomeRepository.deleteByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
	}

	public StatementOutcome updateStatementOutcome(final String namespace, final String municipalityId, final String id, final StatementOutcome statementOutcome) {
		if (!statementOutcomeRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(STATEMENT_OUTCOME, id, namespace, municipalityId));
		}
		if ((statementOutcome.getName() != null) && statementOutcomeRepository.existsByNamespaceAndMunicipalityIdAndNameAndIdNot(namespace, municipalityId, statementOutcome.getName(), id)) {
			throw Problem.valueOf(BAD_REQUEST, ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(STATEMENT_OUTCOME, statementOutcome.getName(), namespace, municipalityId));
		}
		final var entity = updateStatementOutcomeEntity(statementOutcomeRepository.getByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId), statementOutcome);
		return toStatementOutcome(statementOutcomeRepository.save(entity));
	}
}

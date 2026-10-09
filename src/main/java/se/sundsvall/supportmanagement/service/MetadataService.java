package se.sundsvall.supportmanagement.service;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
import se.sundsvall.supportmanagement.api.model.metadata.LabelMergeDryRunResponse;
import se.sundsvall.supportmanagement.api.model.metadata.LabelMergeRequest;
import se.sundsvall.supportmanagement.api.model.metadata.LabelMoveDryRunResponse;
import se.sundsvall.supportmanagement.api.model.metadata.LabelMoveRequest;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureDryRunResponse;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureRequest;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStep;
import se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepResult;
import se.sundsvall.supportmanagement.api.model.metadata.Labels;
import se.sundsvall.supportmanagement.api.model.metadata.MeasureType;
import se.sundsvall.supportmanagement.api.model.metadata.MetadataResponse;
import se.sundsvall.supportmanagement.api.model.metadata.Phase;
import se.sundsvall.supportmanagement.api.model.metadata.PhaseTransition;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.api.model.metadata.StatementOutcome;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
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
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.MERGE;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.MOVE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.MERGE_LABELS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.MOVE_LABEL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.JobType.RESTRUCTURE_LABEL_TREE;
import static se.sundsvall.supportmanagement.service.mapper.LabelClassificationMapper.applyClassificationDisplayNames;
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
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getAdUser;

@Service
public class MetadataService {

	private static final String MEASURE_TYPE_WITHOUT_GROUP = "A measure type must belong to at least one group";
	private static final String ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID = "%s '%s' already exists in namespace '%s' for municipalityId '%s'";
	private static final String ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID = "%s '%s' is not present in namespace '%s' for municipalityId '%s'";
	private static final String LABEL = "Label";
	private static final String HAS_LABEL = "hasLabel";
	private static final String ACTIVE_JOB_IN_NAMESPACE = "A job is already running for namespace '%s' in municipality with id '%s'";
	private static final String COULD_NOT_START = "Label move could not be started: %s";
	private static final String COULD_NOT_START_MERGE = "Label merge could not be started: %s";
	private static final String COULD_NOT_START_RESTRUCTURE = "Label tree restructure could not be started: %s";
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
	private final LabelMergeWorker labelMergeWorker;
	private final LabelTreeRestructureWorker labelTreeRestructureWorker;
	private final AsyncTaskExecutor labelMoveTaskExecutor;
	private final LabelClassificationService labelClassificationService;
	private final AntPathMatcher pathMatcher;
	private final TransactionTemplate readOnlyTransactionTemplate;

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
		// Same cycle, same reason: LabelMergeWorker also sits behind ErrandService.
		@Lazy final LabelMergeWorker labelMergeWorker,
		// Same cycle, same reason: LabelTreeRestructureWorker itself calls into LabelMoveWorker/LabelMergeWorker.
		@Lazy final LabelTreeRestructureWorker labelTreeRestructureWorker,
		@Qualifier("labelMoveTaskExecutor") final AsyncTaskExecutor labelMoveTaskExecutor,
		final LabelClassificationService labelClassificationService,
		final PlatformTransactionManager transactionManager) {
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
		this.labelMergeWorker = labelMergeWorker;
		this.labelTreeRestructureWorker = labelTreeRestructureWorker;
		this.labelMoveTaskExecutor = labelMoveTaskExecutor;
		this.labelClassificationService = labelClassificationService;
		this.pathMatcher = new AntPathMatcher();
		this.pathMatcher.setCaseSensitive(false);
		this.readOnlyTransactionTemplate = new TransactionTemplate(transactionManager);
		this.readOnlyTransactionTemplate.setReadOnly(true);
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
		final var labels = toLabels(metadataLabelRepository.findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId));
		ofNullable(labels).ifPresent(l -> applyClassificationDisplayNames(l.getLabelStructure(), labelClassificationService.getClassificationDisplayNames(namespace, municipalityId)));
		return labels;
	}

	public boolean labelExistsById(final String id, final String namespace, final String municipalityId) {
		return metadataLabelRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
	}

	@Transactional(readOnly = true)
	public LabelMoveDryRunResponse moveLabel(final String namespace, final String municipalityId, final String labelId, final LabelMoveRequest request) {
		var context = validateAndFindLabelToMove(namespace, municipalityId, labelId, request.getNewParentId());
		var allMovedIds = collectMovedLabelIds(context.labelToMove().getId(), context.descendants());

		var affectedErrandCount = errandsRepository.countDistinctByLabelsMetadataLabelIdIn(allMovedIds);
		var affectedActions = actionsReferencing(actionConfigRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId), allMovedIds);

		return LabelMoveDryRunResponse.create()
			.withAffectedErrandCount(affectedErrandCount)
			.withAffectedActions(affectedActions);
	}

	/**
	 * Starts a label move as an asynchronous job, reported through {@code GET .../jobs/{jobId}}.
	 * <p>
	 * The move is validated, and refused with 409 while any label job, of whatever kind, is under way in the namespace.
	 * The job is created with the number of affected errands as its total, and is committed before the move is handed to
	 * {@link LabelMoveWorker} on a thread of its own, so the method is not transactional.
	 */
	public JobResponse startLabelMove(final String namespace, final String municipalityId, final String labelId, final LabelMoveRequest request) {
		var context = readOnlyTransactionTemplate.execute(status -> validateAndFindLabelToMove(namespace, municipalityId, labelId, request.getNewParentId()));
		var canonicalLabelId = context.labelToMove().getId();

		if (jobService.hasActiveJob(namespace, municipalityId)) {
			throw Problem.valueOf(CONFLICT, ACTIVE_JOB_IN_NAMESPACE.formatted(namespace, municipalityId));
		}

		var allMovedIds = collectMovedLabelIds(canonicalLabelId, context.descendants());
		var affectedErrandCount = errandsRepository.countDistinctByLabelsMetadataLabelIdIn(allMovedIds);
		// Committed by the time this call returns, since it is not wrapped in a transaction of this method's own - the
		// worker dispatched right after is free to look the job up from another thread.
		var jobId = jobService.create(namespace, municipalityId, MOVE_LABEL, (int) affectedErrandCount, canonicalLabelId);
		var startedBy = startedBy();
		var startedByAdAccount = nonNull(getAdUser());

		try {
			labelMoveTaskExecutor.execute(() -> labelMoveWorker.run(new LabelMoveRun(jobId, namespace, municipalityId, canonicalLabelId, request.getNewParentId(), startedBy, startedByAdAccount)));
		} catch (final Exception e) {
			// The job is already there and would otherwise sit waiting for a run that never comes.
			jobService.fail(jobId, COULD_NOT_START.formatted(e.getMessage()));

			throw e instanceof final ThrowableProblem problem ? problem : Problem.valueOf(INTERNAL_SERVER_ERROR, COULD_NOT_START.formatted(e.getMessage()));
		}

		return jobService.get(namespace, municipalityId, jobId);
	}

	/**
	 * The caller a label move is recorded against. Read here, on the request thread, since the thread carrying out the
	 * run has no identifier of its own to read.
	 */
	private static String startedBy() {
		return ofNullable(Identifier.get())
			.map(Identifier::getValue)
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
	 * The moved label plus the descendants that move with it, as read by {@link #validateAndFindLabelToMove}.
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
			? newParent.getResourcePath() + LabelTreeSnapshot.SEPARATOR + labelToMove.getResourceName()
			: labelToMove.getResourceName();

		validatePathNotTaken(namespace, municipalityId, labelToMove.getId(), newPath);

		var descendants = metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(
			namespace, municipalityId, labelToMove.getResourcePath() + LabelTreeSnapshot.SEPARATOR);

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
	 * Rejects a new parent that is the moved label itself or one of its descendants. {@code labelId} must be the moved
	 * label's id as stored, not the raw path variable, as the ids are compared case-sensitively.
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
			.ifPresent(_ -> {
				throw Problem.valueOf(CONFLICT, "A label with path '%s' already exists under the destination".formatted(candidatePath));
			});
	}

	/**
	 * Rejects the move with 409 when the new path of any descendant the moved label carries along is held by another
	 * label. Checked before any row is touched.
	 */
	private void validateNoDescendantPathCollision(final String namespace, final String municipalityId, final MetadataLabelEntity labelToMove, final String newPath, final List<MetadataLabelEntity> descendants) {
		var oldPrefixLength = labelToMove.getResourcePath().length();
		descendants.forEach(descendant -> validatePathNotTaken(namespace, municipalityId, descendant.getId(), newPath + descendant.getResourcePath().substring(oldPrefixLength)));
	}

	/**
	 * Rejects the move when the new path of the moved label, or the new path of any descendant it carries along, is longer
	 * than the resource_path column allows. Checked before any row is touched.
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

	/**
	 * Whether an action has a {@code hasLabel} condition naming any of the given label ids - shared by the move and
	 * merge dry-runs, each of which asks it about the set of label ids their own operation would affect.
	 */
	private static boolean referencesAnyLabel(final ActionConfigEntity action, final Set<String> labelIds) {
		return action.getConditions().stream()
			.filter(c -> HAS_LABEL.equals(c.getKey()))
			.flatMap(c -> c.getValues().stream())
			.anyMatch(labelIds::contains);
	}

	@Transactional(readOnly = true)
	public LabelMergeDryRunResponse mergeLabels(final String namespace, final String municipalityId, final String targetLabelId, final LabelMergeRequest request) {
		var context = validateAndFindLabelsToMerge(namespace, municipalityId, targetLabelId, request.getSourceLabelIds());

		var affectedErrandCount = errandsRepository.countDistinctByLabelsMetadataLabelIdIn(context.sourceIds());
		var affectedActions = actionsReferencing(actionConfigRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId), context.sourceIds());

		return LabelMergeDryRunResponse.create()
			.withAffectedErrandCount(affectedErrandCount)
			.withAffectedActions(affectedActions);
	}

	/**
	 * Starts a label merge as an asynchronous job, reported through {@code GET .../jobs/{jobId}}. Mirrors
	 * {@link #startLabelMove} - refused outright if another job is already under way for the namespace, not just
	 * another merge (for the same reason the move guard is type-agnostic: a move or a restructure run touches labels
	 * the same way a merge does, and could race the same tree), and validation, job creation and dispatch are kept as
	 * three separate steps rather than one enclosing transaction, for the same reasons given there.
	 */
	public JobResponse startLabelMerge(final String namespace, final String municipalityId, final String targetLabelId, final LabelMergeRequest request) {
		var context = readOnlyTransactionTemplate.execute(status -> validateAndFindLabelsToMerge(namespace, municipalityId, targetLabelId, request.getSourceLabelIds()));

		if (jobService.hasActiveJob(namespace, municipalityId)) {
			throw Problem.valueOf(CONFLICT, ACTIVE_JOB_IN_NAMESPACE.formatted(namespace, municipalityId));
		}

		var affectedErrandCount = errandsRepository.countDistinctByLabelsMetadataLabelIdIn(context.sourceIds());
		// Committed by the time this call returns, since it is not wrapped in a transaction of this method's own - the
		// worker dispatched right after is free to look the job up from another thread.
		var jobId = jobService.create(namespace, municipalityId, MERGE_LABELS, (int) affectedErrandCount, context.targetId());
		var startedBy = startedBy();
		var startedByAdAccount = nonNull(getAdUser());

		try {
			labelMoveTaskExecutor.execute(() -> labelMergeWorker.run(new LabelMergeRun(jobId, namespace, municipalityId, context.targetId(), context.sourceIds(), startedBy, startedByAdAccount)));
		} catch (final Exception e) {
			// The job is already there and would otherwise sit waiting for a run that never comes.
			jobService.fail(jobId, COULD_NOT_START_MERGE.formatted(e.getMessage()));

			throw e instanceof final ThrowableProblem problem ? problem : Problem.valueOf(INTERNAL_SERVER_ERROR, COULD_NOT_START_MERGE.formatted(e.getMessage()));
		}

		return jobService.get(namespace, municipalityId, jobId);
	}

	/**
	 * The destination label together with the source ids being merged into it - read once by
	 * {@link #validateAndFindLabelsToMerge} and reused by both callers, mirrors {@link LabelMoveContext}.
	 */
	private record LabelMergeContext(String targetId, Set<String> sourceIds) {
	}

	/**
	 * v1 scope is deliberately narrow: both the destination and every source must be leaf labels (no children). Every
	 * real consolidation this exists for merges leaf-level categories; broadening to subtrees is not needed yet and
	 * would have to decide how descendants of several sources fold together, which a leaf-only merge sidesteps
	 * entirely.
	 */
	private LabelMergeContext validateAndFindLabelsToMerge(final String namespace, final String municipalityId, final String targetLabelId, final List<String> requestedSourceIds) {
		var target = metadataLabelRepository.findByIdAndNamespaceAndMunicipalityId(targetLabelId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(LABEL, targetLabelId, namespace, municipalityId)));
		validateIsLeaf(namespace, municipalityId, target);

		// Built from each looked-up label's own stored id, never from the raw strings the client sent: a source id
		// differing from the target's only in case would otherwise still resolve to the very same row below (a
		// case-insensitive id lookup/collation) while failing the self-merge check above it, which compares the raw
		// string against target.getId() verbatim - letting the destination label itself slip into sourceIds and, once
		// every real source is restowed away, get deleted right along with them.
		var sourceIds = requestedSourceIds.stream()
			.map(sourceId -> metadataLabelRepository.findByIdAndNamespaceAndMunicipalityId(sourceId, namespace, municipalityId)
				.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID.formatted(LABEL, sourceId, namespace, municipalityId))))
			.peek(source -> validateIsLeaf(namespace, municipalityId, source))
			.map(MetadataLabelEntity::getId)
			.collect(toSet());

		if (sourceIds.contains(target.getId())) {
			throw Problem.valueOf(BAD_REQUEST, "Label '%s' cannot be merged into itself".formatted(target.getId()));
		}

		return new LabelMergeContext(target.getId(), sourceIds);
	}

	private void validateIsLeaf(final String namespace, final String municipalityId, final MetadataLabelEntity label) {
		if (metadataLabelRepository.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(namespace, municipalityId, label.getResourcePath() + LabelTreeSnapshot.SEPARATOR)) {
			throw Problem.valueOf(BAD_REQUEST, "Label '%s' has children and cannot take part in a merge".formatted(label.getId()));
		}
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
	 * The labels of the namespace are read once for all groups, and not at all when no group carries a pattern.
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
	// Label tree restructure operations
	// =================================================================

	/**
	 * Validates and reports the effect of every step in {@code request} without making any change - mirrors
	 * {@link #moveLabel}/{@link #mergeLabels}.
	 */
	@Transactional(readOnly = true)
	public LabelRestructureDryRunResponse restructureLabelTree(final String namespace, final String municipalityId, final LabelRestructureRequest request) {
		final var results = simulateSteps(namespace, municipalityId, request.getSteps());
		final var total = results.stream().mapToLong(LabelRestructureStepResult::getAffectedErrandCount).sum();

		return LabelRestructureDryRunResponse.create()
			.withTotalAffectedErrandCount(total)
			.withSteps(results);
	}

	/**
	 * Starts a label-tree restructure as an asynchronous job, reported through {@code GET .../jobs/{jobId}}. Mirrors
	 * {@link #startLabelMove}/{@link #startLabelMerge}: validation (here, the same step-by-step simulation
	 * {@link #restructureLabelTree} uses, so every step is checked up front rather than discovered mid-run), job
	 * creation and dispatch are kept as separate steps rather than one enclosing transaction, for the same reasons
	 * given there.
	 * <p>
	 * Guarded by the type-agnostic {@link JobService#hasActiveJob(String, String)} rather than one scoped to
	 * {@code RESTRUCTURE_LABEL_TREE} - a restructure carries out many moves and merges over the course of one run, so it
	 * must not be started alongside a lone {@code MOVE_LABEL}/{@code MERGE_LABELS} job racing the same tree either.
	 * {@link #startLabelMove}/{@link #startLabelMerge} guard the other direction the same way.
	 */
	public JobResponse startLabelTreeRestructure(final String namespace, final String municipalityId, final LabelRestructureRequest request) {
		final var results = simulateSteps(namespace, municipalityId, request.getSteps());

		if (jobService.hasActiveJob(namespace, municipalityId)) {
			throw Problem.valueOf(CONFLICT, ACTIVE_JOB_IN_NAMESPACE.formatted(namespace, municipalityId));
		}

		final var estimatedTotal = (int) results.stream().mapToLong(LabelRestructureStepResult::getAffectedErrandCount).sum();
		// Committed by the time this call returns, since it is not wrapped in a transaction of this method's own - the
		// worker dispatched right after is free to look the job up from another thread.
		final var jobId = jobService.create(namespace, municipalityId, RESTRUCTURE_LABEL_TREE, estimatedTotal);
		final var startedBy = startedBy();
		final var startedByAdAccount = nonNull(getAdUser());

		try {
			labelMoveTaskExecutor.execute(() -> labelTreeRestructureWorker.run(new LabelRestructureRun(jobId, namespace, municipalityId, request.getSteps(), startedBy, startedByAdAccount)));
		} catch (final Exception e) {
			// The job is already there and would otherwise sit waiting for a run that never comes.
			jobService.fail(jobId, COULD_NOT_START_RESTRUCTURE.formatted(e.getMessage()));

			throw e instanceof final ThrowableProblem problem ? problem : Problem.valueOf(INTERNAL_SERVER_ERROR, COULD_NOT_START_RESTRUCTURE.formatted(e.getMessage()));
		}

		return jobService.get(namespace, municipalityId, jobId);
	}

	/**
	 * Walks {@code steps} in order against a {@link LabelTreeSnapshot} built once from the namespace's current label
	 * tree, so that a step referencing a label an earlier step in this same list added or moved resolves against what
	 * that earlier step did, not against what is actually persisted yet. Used both for a pure dry-run
	 * ({@link #restructureLabelTree}) and as {@link #startLabelTreeRestructure}'s up-front validation pass - neither
	 * call persists anything itself, that is {@code LabelTreeRestructureWorker}'s job once a run is actually dispatched.
	 */
	private List<LabelRestructureStepResult> simulateSteps(final String namespace, final String municipalityId, final List<LabelRestructureStep> steps) {
		final var snapshot = LabelTreeSnapshot.of(metadataLabelRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId));
		// Fetched once up front, rather than once per MOVE/MERGE step - every step in the same request asks the same
		// namespace the same question, and fetched at all only when some step actually needs it.
		final var needsActions = steps.stream().anyMatch(step -> (step.getType() == MOVE) || (step.getType() == MERGE));
		final var actions = needsActions ? actionConfigRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId) : List.<ActionConfigEntity>of();
		final var results = new ArrayList<LabelRestructureStepResult>();

		for (var index = 0; index < steps.size(); index++) {
			results.add(simulateStep(snapshot, actions, steps.get(index), index));
		}

		return results;
	}

	private LabelRestructureStepResult simulateStep(final LabelTreeSnapshot snapshot, final List<ActionConfigEntity> actions, final LabelRestructureStep step, final int index) {
		return switch (step.getType()) {
			case ADD -> simulateAdd(snapshot, step, index);
			case RENAME -> simulateRename(snapshot, step, index);
			case DELETE -> simulateDelete(snapshot, step, index);
			case MOVE -> simulateMove(snapshot, actions, step, index);
			case MERGE -> simulateMerge(snapshot, actions, step, index);
		};
	}

	private static LabelRestructureStepResult stepResult(final LabelRestructureStep step, final int index, final long affectedErrandCount, final List<AffectedAction> affectedActions) {
		return LabelRestructureStepResult.create()
			.withIndex(index)
			.withType(step.getType())
			.withPath(step.getPath())
			.withAffectedErrandCount(affectedErrandCount)
			.withAffectedActions(affectedActions);
	}

	/**
	 * Never affects an existing errand - a label freshly added by this step cannot be referenced by anything yet.
	 * A no-op (this step's path already exists) is not an error - resubmitting a request whose earlier steps already
	 * ran must be safe, mirroring the reference {@code ange_kommun_category_restructure.py} runbook's own
	 * "skip add, already present" behaviour.
	 */
	private LabelRestructureStepResult simulateAdd(final LabelTreeSnapshot snapshot, final LabelRestructureStep step, final int index) {
		final var path = LabelTreeSnapshot.join(step.getPath());

		if (!snapshot.exists(path)) {
			final var parentPath = LabelTreeSnapshot.join(LabelTreeSnapshot.allButLast(step.getPath()));
			if (!snapshot.exists(parentPath)) {
				throw Problem.valueOf(BAD_REQUEST, "Step %d (ADD): parent path '%s' does not exist".formatted(index, parentPath));
			}
			rejectIfTooLong(path);
			snapshot.recordAdd(path, null);
		}

		return stepResult(step, index, 0, emptyList());
	}

	/**
	 * Never restows anything or changes an action's conditions - the id and resourceName a merge/move/action condition
	 * would reference are untouched by a display-name-only rename.
	 */
	private LabelRestructureStepResult simulateRename(final LabelTreeSnapshot snapshot, final LabelRestructureStep step, final int index) {
		final var path = LabelTreeSnapshot.join(step.getPath());
		if (!snapshot.exists(path)) {
			throw Problem.valueOf(NOT_FOUND, "Step %d (RENAME): label at path '%s' does not exist".formatted(index, path));
		}

		return stepResult(step, index, 0, emptyList());
	}

	/**
	 * Mirrors the errand-reference guard {@link #updateLabels} already applies, and the "never deleted blindly, only
	 * once actually empty" caution the reference runbook's cleanup step follows - a DELETE step for a category emptied
	 * by earlier steps in the same request only succeeds once those earlier steps have already emptied it.
	 */
	private LabelRestructureStepResult simulateDelete(final LabelTreeSnapshot snapshot, final LabelRestructureStep step, final int index) {
		final var path = LabelTreeSnapshot.join(step.getPath());
		if (!snapshot.exists(path)) {
			throw Problem.valueOf(NOT_FOUND, "Step %d (DELETE): label at path '%s' does not exist".formatted(index, path));
		}
		if (!snapshot.isLeaf(path)) {
			throw Problem.valueOf(BAD_REQUEST, "Step %d (DELETE): label at path '%s' still has children".formatted(index, path));
		}

		// accessLabels, not labels: labels carries the full ancestor chain, so an errand tagged with a child this path
		// used to have would still show up against this path's own id there even after an earlier step in this same
		// request already moved that child elsewhere - accessLabels holds only an errand's own leaf tags, which earlier
		// steps already elsewhere in the tree do not touch. realIdsAtOrUnder rather than a plain idAt, so an id an
		// earlier MERGE step folded into this exact (leaf) path is still asked about too.
		final var realIds = snapshot.realIdsAtOrUnder(path);
		if (!realIds.isEmpty() && errandsRepository.existsByAccessLabelsMetadataLabelIdIn(realIds)) {
			throw Problem.valueOf(BAD_REQUEST, "Step %d (DELETE): label at path '%s' is referenced by one or more errands".formatted(index, path));
		}

		snapshot.recordDelete(path);
		return stepResult(step, index, 0, emptyList());
	}

	private LabelRestructureStepResult simulateMove(final LabelTreeSnapshot snapshot, final List<ActionConfigEntity> actions, final LabelRestructureStep step, final int index) {
		final var sourcePath = LabelTreeSnapshot.join(step.getPath());
		if (!snapshot.exists(sourcePath)) {
			throw Problem.valueOf(NOT_FOUND, "Step %d (MOVE): label at path '%s' does not exist".formatted(index, sourcePath));
		}

		final var destinationParentSegments = ofNullable(step.getDestinationParentPath()).orElse(emptyList());
		final var destinationParentPath = LabelTreeSnapshot.join(destinationParentSegments);
		if (!snapshot.exists(destinationParentPath)) {
			throw Problem.valueOf(BAD_REQUEST, "Step %d (MOVE): destination parent path '%s' does not exist".formatted(index, destinationParentPath));
		}

		final var newResourceName = ofNullable(step.getNewResourceName()).orElseGet(() -> LabelTreeSnapshot.lastSegment(step.getPath()));
		final var newPath = destinationParentPath.isEmpty() ? newResourceName : destinationParentPath + LabelTreeSnapshot.SEPARATOR + newResourceName;
		final var currentParentPath = LabelTreeSnapshot.join(LabelTreeSnapshot.allButLast(step.getPath()));

		if (currentParentPath.equals(destinationParentPath) && newResourceName.equals(LabelTreeSnapshot.lastSegment(step.getPath()))) {
			throw Problem.valueOf(BAD_REQUEST, "Step %d (MOVE): label at path '%s' is already at that destination - move would be a no-op".formatted(index, sourcePath));
		}
		if (newPath.equals(sourcePath) || destinationParentPath.equals(sourcePath) || destinationParentPath.startsWith(sourcePath + LabelTreeSnapshot.SEPARATOR)) {
			throw Problem.valueOf(BAD_REQUEST, "Step %d (MOVE): moving '%s' under '%s' would create a cycle".formatted(index, sourcePath, destinationParentPath));
		}
		if (snapshot.exists(newPath)) {
			throw Problem.valueOf(CONFLICT, "Step %d (MOVE): a label already exists at destination path '%s'".formatted(index, newPath));
		}
		rejectIfTooLong(newPath);
		validateDescendantPathsAfterMove(snapshot, sourcePath, newPath, index);

		final var realIds = snapshot.realIdsAtOrUnder(sourcePath);
		final var affected = affectedBy(realIds, actions);

		snapshot.recordMove(sourcePath, newPath);
		return stepResult(step, index, affected.errandCount(), affected.actions());
	}

	/**
	 * A descendant's resulting path can collide or overflow just as easily as the moved label's own, since both land
	 * under a destination neither of them has occupied before - checked here the same way
	 * {@link #validateNoDescendantPathCollision}/{@link #validateResourcePathLength} check it for the standalone move
	 * endpoint, so a dry run and the up-front validation {@link #startLabelTreeRestructure} runs before dispatching a
	 * worker cannot report success on a step that would later fail mid-run against the DB's own path constraints.
	 */
	private void validateDescendantPathsAfterMove(final LabelTreeSnapshot snapshot, final String sourcePath, final String newPath, final int index) {
		final var oldPrefixLength = sourcePath.length();
		for (final var descendantPath : snapshot.descendantPathsUnder(sourcePath)) {
			final var rebasedPath = newPath + descendantPath.substring(oldPrefixLength);
			rejectIfTooLong(rebasedPath);
			if (snapshot.exists(rebasedPath)) {
				throw Problem.valueOf(CONFLICT, "Step %d (MOVE): moving '%s' would collide with an existing label at '%s'".formatted(index, sourcePath, rebasedPath));
			}
		}
	}

	private LabelRestructureStepResult simulateMerge(final LabelTreeSnapshot snapshot, final List<ActionConfigEntity> actions, final LabelRestructureStep step, final int index) {
		final var targetPath = LabelTreeSnapshot.join(step.getPath());
		if (!snapshot.exists(targetPath)) {
			throw Problem.valueOf(NOT_FOUND, "Step %d (MERGE): destination label at path '%s' does not exist".formatted(index, targetPath));
		}
		if (!snapshot.isLeaf(targetPath)) {
			throw Problem.valueOf(BAD_REQUEST, "Step %d (MERGE): destination label at path '%s' has children and cannot take part in a merge".formatted(index, targetPath));
		}

		final var sourcePaths = step.getSourcePaths().stream().map(LabelTreeSnapshot::join).toList();
		if (sourcePaths.contains(targetPath)) {
			throw Problem.valueOf(BAD_REQUEST, "Step %d (MERGE): label at path '%s' cannot be merged into itself".formatted(index, targetPath));
		}
		sourcePaths.forEach(sourcePath -> {
			if (!snapshot.exists(sourcePath)) {
				throw Problem.valueOf(BAD_REQUEST, "Step %d (MERGE): source label at path '%s' does not exist".formatted(index, sourcePath));
			}
			if (!snapshot.isLeaf(sourcePath)) {
				throw Problem.valueOf(BAD_REQUEST, "Step %d (MERGE): source label at path '%s' has children and cannot take part in a merge".formatted(index, sourcePath));
			}
		});

		// realIdsAtOrUnder rather than idAt: a source path targeted by an earlier MERGE step in this same request may
		// itself already carry along ids folded into it from a still-earlier source - see LabelTreeSnapshot's own doc.
		final var realSourceIds = sourcePaths.stream().flatMap(sourcePath -> snapshot.realIdsAtOrUnder(sourcePath).stream()).collect(toSet());
		final var affected = affectedBy(realSourceIds, actions);

		snapshot.recordMerge(targetPath, sourcePaths);
		return stepResult(step, index, affected.errandCount(), affected.actions());
	}

	/**
	 * How many errands and which actions {@code labelIds} affects - shared by {@link #simulateMove}/
	 * {@link #simulateMerge}, which each ask it about the set of real (persisted) label ids their own step would
	 * touch. A set built entirely from labels an earlier {@code ADD} step in the same request created has no real ids
	 * to ask the database about yet, hence the short-circuit rather than querying with an empty set.
	 */
	private AffectedLabels affectedBy(final Set<String> labelIds, final List<ActionConfigEntity> actions) {
		if (labelIds.isEmpty()) {
			return new AffectedLabels(0L, List.of());
		}
		return new AffectedLabels(errandsRepository.countDistinctByLabelsMetadataLabelIdIn(labelIds), actionsReferencing(actions, labelIds));
	}

	private record AffectedLabels(long errandCount, List<AffectedAction> actions) {
	}

	/**
	 * Shared by {@link #simulateMove}/{@link #simulateMerge} - both need "which of this namespace's actions have a
	 * hasLabel condition naming any of these ids", the same question {@link #moveLabel}/{@link #mergeLabels} already
	 * ask via {@link #referencesAnyLabel} for the standalone endpoints. Filters {@code actions} rather than querying
	 * the namespace itself, since {@link #simulateSteps} already fetched it once for the whole request - a restructure
	 * with several MOVE/MERGE steps must not re-run that same namespace-wide query once per step.
	 */
	private static List<AffectedAction> actionsReferencing(final List<ActionConfigEntity> actions, final Set<String> labelIds) {
		return actions.stream()
			.filter(action -> referencesAnyLabel(action, labelIds))
			.map(action -> AffectedAction.create()
				.withId(action.getId())
				.withName(action.getName())
				.withDisplayValue(action.getDisplayValue()))
			.toList();
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
	 * Rejects a measure type that names no group. A measure type belongs to at least one group.
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

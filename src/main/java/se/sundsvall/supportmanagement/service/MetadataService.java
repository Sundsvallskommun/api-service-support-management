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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.api.model.metadata.AffectedAction;
import se.sundsvall.supportmanagement.api.model.metadata.AttachmentPurpose;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.api.model.metadata.DecisionOutcome;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
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
import se.sundsvall.supportmanagement.config.JobProperties;
import se.sundsvall.supportmanagement.config.LabelRestructureProperties;
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
import se.sundsvall.supportmanagement.model.enums.EntityType;
import se.sundsvall.supportmanagement.service.job.JobService;
import se.sundsvall.supportmanagement.service.job.JobSpec;
import se.sundsvall.supportmanagement.service.job.LabelRestructureRun;
import se.sundsvall.supportmanagement.service.job.LabelRestructureRunner;
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
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.CollectionUtils.isEmpty;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.MERGE;
import static se.sundsvall.supportmanagement.api.model.metadata.LabelRestructureStepType.MOVE;
import static se.sundsvall.supportmanagement.model.enums.JobType.RESTRUCTURE_LABEL_TREE;
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
	private static final String TOO_MANY_AFFECTED_ERRANDS = "Restructure affects %d errand(s), exceeding the maximum of %d allowed in one restructure";
	private static final String COULD_NOT_START_RESTRUCTURE = "Label tree restructure could not be started: %s";
	private static final String UNKNOWN_CALLER = "unknown";
	private static final int RESOURCE_PATH_MAX_LENGTH = 255;
	private static final String LABEL_GONE_BY_ID = "Label %s no longer exists";
	private static final String LABEL_GONE_BY_PATH = "Label at path '%s' no longer exists";
	private static final String PARENT_GONE = "Parent at path '%s' no longer exists";
	private static final String HAS_CHILDREN = "Label at path '%s' still has children";
	private static final String REFERENCED_BY_ERRANDS = "Label at path '%s' is referenced by one or more errands";

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
	private final LabelRestructureRunner labelRestructureRunner;
	private final AsyncTaskExecutor labelRestructureTaskExecutor;
	private final LabelClassificationService labelClassificationService;
	private final AntPathMatcher pathMatcher;
	private final Duration jobStaleAfter;
	private final LabelRestructureProperties labelRestructureProperties;

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
		// Lazy: LabelRestructureRunner sits behind ErrandService -> RevisionService -> AccessControlService ->
		// AccessMapperService -> MetadataService, a cycle back to this very bean. Never actually needed before the
		// async dispatch fires, by which point every bean in the cycle is already constructed.
		@Lazy final LabelRestructureRunner labelRestructureRunner,
		@Qualifier("labelRestructureTaskExecutor") final AsyncTaskExecutor labelRestructureTaskExecutor,
		final LabelClassificationService labelClassificationService,
		final JobProperties jobProperties,
		final LabelRestructureProperties labelRestructureProperties) {
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
		this.labelRestructureRunner = labelRestructureRunner;
		this.labelRestructureTaskExecutor = labelRestructureTaskExecutor;
		this.labelClassificationService = labelClassificationService;
		this.pathMatcher = new AntPathMatcher();
		this.pathMatcher.setCaseSensitive(false);
		this.jobStaleAfter = jobProperties.staleAfter();
		this.labelRestructureProperties = labelRestructureProperties;
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

	/**
	 * The caller a label-tree mutation is recorded against. Read here, on the request thread, since the thread
	 * carrying out the run has no identifier of its own to read.
	 * <p>
	 * Carries the whole identifier - type and value, via {@link Identifier#toHeaderValue()} - rather than just the
	 * value, so that {@link EventService#createLabelMoveEvent}/{@link EventService#createLabelMergeEvent} can rebuild
	 * the original {@link Identifier} instead of defaulting the type to {@code CUSTOM} and misrepresenting an AD
	 * user's account name as a party id in the audit trail.
	 */
	private static String startedBy() {
		return ofNullable(Identifier.get())
			.map(Identifier::toHeaderValue)
			.orElse(UNKNOWN_CALLER);
	}

	/**
	 * Re-validates and re-parents the label, inside the runner's own transaction, immediately before the write - not
	 * the snapshot-based validation {@link #simulateSteps} ran when the restructure was accepted, but the same kind
	 * of execution-time re-check every step type now gets (see {@link #mergeInto}/{@link #addLabel}/
	 * {@link #renameLabel}/{@link #deleteLabel}). A concurrent {@code PUT .../labels} (a wholesale replace of the
	 * tree) landing between that validation and this point could otherwise turn a cycle, a path collision, or an
	 * oversized path into an unhandled exception partway through the write, instead of the same clear failure a
	 * fresh request would have been refused with up front.
	 * <p>
	 * {@code newResourceName}/{@code newDisplayName}, when given (non-null), are set in the same update - the
	 * combined move+rename a restructure's MOVE step allows but a plain reparent does not need.
	 * <p>
	 * Called by {@link se.sundsvall.supportmanagement.service.job.LabelRestructureRunner}, a different package from
	 * this one - the same reason {@link #mergeInto} is public.
	 */
	@Transactional
	public void revalidateAndReparent(final String namespace, final String municipalityId, final String labelId, final String newParentId, final String newResourceName, final String newDisplayName) {
		var labelToMove = metadataLabelRepository.findById(labelId)
			.orElseThrow(() -> new IllegalStateException(LABEL_GONE_BY_ID.formatted(labelId)));

		MetadataLabelEntity newParent = null;
		if (newParentId != null) {
			newParent = metadataLabelRepository.findById(newParentId)
				.orElseThrow(() -> new IllegalStateException(LABEL_GONE_BY_ID.formatted(newParentId)));
		}

		validateNotNoOp(labelToMove, newParentId, newResourceName);
		validateNoCycle(labelToMove.getId(), newParent);

		var resourceName = newResourceName != null ? newResourceName : labelToMove.getResourceName();
		var newPath = newParent != null
			? newParent.getResourcePath() + LabelTreeSnapshot.SEPARATOR + resourceName
			: resourceName;

		validatePathNotTaken(namespace, municipalityId, labelToMove.getId(), newPath);

		var descendants = metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(
			namespace, municipalityId, labelToMove.getResourcePath() + LabelTreeSnapshot.SEPARATOR);

		validateNoDescendantPathCollision(namespace, municipalityId, labelToMove, newPath, descendants);
		validateResourcePathLength(labelToMove, newPath, descendants);

		labelToMove.setParent(newParent);
		if (newResourceName != null) {
			labelToMove.setResourceName(newResourceName);
		}
		if (newDisplayName != null) {
			labelToMove.setDisplayName(newDisplayName);
		}
		metadataLabelRepository.saveAndFlush(labelToMove);
		// The @PreUpdate cascade on labelToMove (onUpdate -> updateChildrenPathsRecursively) recomputes resourcePath for
		// the moved node and, recursively, for every descendant reachable through its metadataLabels collection, all
		// within this single saveAndFlush's own session - the whole subtree is already correct and persisted by the
		// time this call returns.
	}

	/**
	 * A move to the label's current parent, with no resourceName change, is rejected as a no-op - mirrored by
	 * {@code simulateMove}'s own no-op check against the request-time snapshot, which must agree with this one: a
	 * restructure's MOVE step that only renames (new {@code displayName}, same parent and {@code resourceName}) is
	 * not a no-op, since a rename-only step is exactly what a MOVE step combining a rename with no actual move is
	 * for.
	 */
	private static void validateNotNoOp(final MetadataLabelEntity labelToMove, final String newParentId, final String newResourceName) {
		var currentParentId = labelToMove.getParent() != null ? labelToMove.getParent().getId() : null;
		var resourceNameUnchanged = newResourceName == null || Objects.equals(labelToMove.getResourceName(), newResourceName);
		if (Objects.equals(currentParentId, newParentId) && resourceNameUnchanged) {
			throw new IllegalStateException("Label '%s' is already under the specified parent and name - move would be a no-op".formatted(labelToMove.getId()));
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

	private void validateIsLeaf(final String namespace, final String municipalityId, final MetadataLabelEntity label) {
		if (metadataLabelRepository.existsByNamespaceAndMunicipalityIdAndResourcePathStartingWith(namespace, municipalityId, label.getResourcePath() + LabelTreeSnapshot.SEPARATOR)) {
			throw new IllegalStateException("Label '%s' has children and cannot take part in a merge".formatted(label.getId()));
		}
	}

	/**
	 * Retargets every {@code hasLabel} action condition naming one of {@code sourceLabelIds} onto
	 * {@code targetLabelId}, inside the caller's own transaction - the mutation half of a MERGE step. Re-validates
	 * target and sources (existence, leaf-ness, self-merge) immediately before writing, the execution-time re-check
	 * every step type now gets - see {@link #revalidateAndReparent}'s own doc.
	 * <p>
	 * Restowing affected errands and deleting the now-unreferenced source labels are deliberately not done here: the
	 * restructure runner restows once, at the end, over the union of every MOVE/MERGE step's affected errands (so an
	 * errand touched by two steps is restowed once, not twice), and the source labels must stay in place until that
	 * restow has read the tree they still define - deleted only afterward, by {@link #deleteMergedLabels}, once
	 * every affected errand has already been substituted off them.
	 * <p>
	 * Public rather than package-private: {@link se.sundsvall.supportmanagement.service.job.LabelRestructureRunner},
	 * its only caller, lives in {@code se.sundsvall.supportmanagement.service.job} - a different package from this
	 * one - the same reason {@link #revalidateAndReparent} is public.
	 */
	@Transactional
	public void mergeInto(final String namespace, final String municipalityId, final String targetLabelId, final Set<String> sourceLabelIds) {
		var target = metadataLabelRepository.findById(targetLabelId)
			.orElseThrow(() -> new IllegalStateException(LABEL_GONE_BY_ID.formatted(targetLabelId)));
		validateIsLeaf(namespace, municipalityId, target);

		if (sourceLabelIds.contains(target.getId())) {
			throw new IllegalStateException("Label '%s' cannot be merged into itself".formatted(target.getId()));
		}

		sourceLabelIds.forEach(sourceId -> {
			var source = metadataLabelRepository.findById(sourceId)
				.orElseThrow(() -> new IllegalStateException(LABEL_GONE_BY_ID.formatted(sourceId)));
			validateIsLeaf(namespace, municipalityId, source);
		});

		retargetActionConditions(namespace, municipalityId, sourceLabelIds, targetLabelId);
	}

	/**
	 * Replaces every source label id a {@code hasLabel} action condition named with the destination's id, so a merge
	 * does not leave a condition quietly pointing at a row that is about to be deleted. Deduplicated afterward: a
	 * condition naming both a source and the destination, or naming two sources now folding into the same id, must
	 * not end up with the destination id repeated.
	 */
	private void retargetActionConditions(final String namespace, final String municipalityId, final Set<String> sourceLabelIds, final String targetLabelId) {
		final var changed = new ArrayList<ActionConfigEntity>();

		actionConfigRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId).forEach(config -> {
			final var configChanged = new boolean[] {
				false
			};
			config.getConditions().stream()
				.filter(condition -> HAS_LABEL.equals(condition.getKey()))
				.filter(condition -> condition.getValues().stream().anyMatch(sourceLabelIds::contains))
				.forEach(condition -> {
					condition.setValues(condition.getValues().stream()
						.map(value -> sourceLabelIds.contains(value) ? targetLabelId : value)
						.distinct()
						.toList());
					configChanged[0] = true;
				});
			if (configChanged[0]) {
				changed.add(config);
			}
		});

		if (!changed.isEmpty()) {
			actionConfigRepository.saveAll(changed);
		}
	}

	/**
	 * Deletes every label in {@code labelIds}, taking each out of its parent's children collection first and
	 * flushing immediately, inside the caller's own transaction - a parent loaded into the same session with that
	 * collection initialized would otherwise persist a removed child again when the session next flushes on its own,
	 * quietly undoing the delete. Called once, by
	 * {@link se.sundsvall.supportmanagement.service.job.LabelRestructureRunner},
	 * after every affected errand in the run has already been restowed off these labels - never before, since restow
	 * still needs to read the tree they define.
	 */
	@Transactional
	public void deleteMergedLabels(final Set<String> labelIds) {
		metadataLabelRepository.findAllById(labelIds).forEach(label -> {
			ofNullable(label.getParent()).ifPresent(parent -> parent.getMetadataLabels().remove(label));
			metadataLabelRepository.delete(label);
		});
		metadataLabelRepository.flush();
	}

	/**
	 * Adds a label at {@code path}, inside the caller's own transaction - the mutation half of an ADD step. A no-op
	 * if the path already exists - safe to resubmit a request whose earlier attempt already added this label,
	 * mirroring {@code simulateAdd}'s own dry-run treatment of the same case.
	 * <p>
	 * {@code MetadataLabelEntity}'s {@code @PrePersist} walks the new label's whole ancestor chain to compute
	 * {@code resourcePath}, following {@code parent.getParent()} as many levels up as the tree goes - the parent
	 * looked up here must therefore come from, and the save must run inside, the same session this already shares
	 * with every other step in the same run.
	 */
	@Transactional
	public void addLabel(final String namespace, final String municipalityId, final List<String> path, final String displayName, final String classification) {
		final var resolvedPath = LabelTreeSnapshot.join(path);
		if (metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePath(namespace, municipalityId, resolvedPath).isPresent()) {
			return;
		}

		final var parentPath = LabelTreeSnapshot.join(LabelTreeSnapshot.allButLast(path));
		final var parent = parentPath.isEmpty() ? null : findOrThrow(namespace, municipalityId, parentPath, PARENT_GONE);

		metadataLabelRepository.save(MetadataLabelEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withClassification(classification)
			.withDisplayName(displayName)
			.withResourceName(LabelTreeSnapshot.lastSegment(path))
			.withParent(parent));
	}

	/**
	 * Renames the label at {@code path} to {@code displayName}, inside the caller's own transaction - the mutation
	 * half of a RENAME step. Never restows anything or changes an action's conditions - the id and resourceName a
	 * merge/move/action condition would reference are untouched by a display-name-only rename.
	 */
	@Transactional
	public void renameLabel(final String namespace, final String municipalityId, final List<String> path, final String displayName) {
		final var entity = findOrThrow(namespace, municipalityId, LabelTreeSnapshot.join(path), LABEL_GONE_BY_PATH);
		entity.setDisplayName(displayName);
		metadataLabelRepository.save(entity);
	}

	/**
	 * Deletes the label at {@code path}, inside the caller's own transaction - the mutation half of a DELETE step.
	 * Re-validates "no children" and "not referenced by an errand" immediately before writing, mirroring the
	 * errand-reference guard {@link #updateLabels} already applies.
	 */
	@Transactional
	public void deleteLabel(final String namespace, final String municipalityId, final List<String> path) {
		final var resolvedPath = LabelTreeSnapshot.join(path);
		final var entity = findOrThrow(namespace, municipalityId, resolvedPath, LABEL_GONE_BY_PATH);

		if (!metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(namespace, municipalityId, resolvedPath + LabelTreeSnapshot.SEPARATOR).isEmpty()) {
			throw new IllegalStateException(HAS_CHILDREN.formatted(resolvedPath));
		}
		if (errandsRepository.existsByLabelsMetadataLabelIdIn(Set.of(entity.getId()))) {
			throw new IllegalStateException(REFERENCED_BY_ERRANDS.formatted(resolvedPath));
		}

		metadataLabelRepository.deleteById(entity.getId());
	}

	private MetadataLabelEntity findOrThrow(final String namespace, final String municipalityId, final String path, final String messageTemplate) {
		return metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePath(namespace, municipalityId, path)
			.orElseThrow(() -> new IllegalStateException(messageTemplate.formatted(path)));
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
	 * Validates and reports the effect of every step in {@code request} without making any change.
	 */
	@Transactional(readOnly = true)
	public LabelRestructureDryRunResponse restructureLabelTree(final String namespace, final String municipalityId, final LabelRestructureRequest request) {
		final var simulation = simulateSteps(namespace, municipalityId, request.getSteps());
		final var total = simulation.results().stream().mapToLong(LabelRestructureStepResult::getAffectedErrandCount).sum();

		return LabelRestructureDryRunResponse.create()
			.withTotalAffectedErrandCount(total)
			.withSteps(simulation.results());
	}

	/**
	 * Starts a label-tree restructure as an asynchronous job, reported through {@code GET .../jobs/{jobId}}:
	 * validation (here, the same step-by-step simulation {@link #restructureLabelTree} uses, so every step is
	 * checked up front rather than discovered mid-run), job creation and dispatch are kept as separate steps rather
	 * than one enclosing transaction - wrapping this whole method in one transaction would leave the job it creates
	 * stuck PENDING, since {@link JobService#launch} dispatches to another thread before this method returns.
	 * <p>
	 * Asked first: {@link JobService#stealStaleLease}, so a restructure job whose instance died mid-run does not go
	 * on blocking the namespace for as long as {@link JobProperties#staleAfter()} allows - only until the next
	 * caller tries to start one, at which point the stale lease is reclaimed on the spot (committed before this
	 * method returns to it) and the check right after no longer sees it. A crashed instance's own connection is
	 * rolled back cleanly by the database the moment it drops - the whole run is one transaction (see
	 * {@code LabelRestructureRunner}), so there is no half-done state to resume - this only shrinks how long the
	 * namespace stays blocked waiting for a fresh attempt.
	 * <p>
	 * The affected-errand count used for both the {@link #labelRestructureProperties}' cap and the job's own
	 * {@code total} is the deduplicated union across every MOVE/MERGE step - an errand touched by more than one step
	 * is counted, and later restowed, once - unlike {@link #restructureLabelTree}'s own
	 * {@code totalAffectedErrandCount}, which deliberately sums each step's own count instead (see its own doc).
	 */
	public JobResponse startLabelTreeRestructure(final String namespace, final String municipalityId, final LabelRestructureRequest request) {
		final var simulation = simulateSteps(namespace, municipalityId, request.getSteps());

		// Both asked unconditionally, rather than short-circuited: a stale lease reclaimed by the first call must not
		// suppress the second.
		final var reclaimed = jobService.stealStaleLease(namespace, municipalityId, RESTRUCTURE_LABEL_TREE, jobStaleAfter);
		final var activeInNamespace = jobService.hasActiveJob(namespace, municipalityId);
		if (!reclaimed || activeInNamespace) {
			throw Problem.valueOf(CONFLICT, ACTIVE_JOB_IN_NAMESPACE.formatted(namespace, municipalityId));
		}

		// Resolved once, here - the job's own total and the walk that restows them both read from this exact list
		// rather than each re-deriving their own (see LabelRestructureRun#errandIds for why that matters). Frozen at
		// this point deliberately: an errand created after this is created against the tree the restructure already
		// left in place, so it needs no restowing.
		final var affectedErrandIds = simulation.touchedLabelIds().isEmpty()
			? List.<String>of()
			: errandsRepository.findDistinctIdsByLabelsMetadataLabelIdIn(simulation.touchedLabelIds());

		// Refused here, before any lock is taken rather than left unbounded: the restructure locks every one of these
		// errands for the life of one transaction (see LabelRestructureRunner), so an unexpectedly large affected set
		// would otherwise hold that many rows for however long restowing them all takes. The dry run deliberately has
		// no such limit - it takes no lock, and is exactly what lets an admin discover they are over the cap before
		// ever trying.
		if (affectedErrandIds.size() > labelRestructureProperties.maxAffectedErrands()) {
			throw Problem.valueOf(BAD_REQUEST, TOO_MANY_AFFECTED_ERRANDS.formatted(affectedErrandIds.size(), labelRestructureProperties.maxAffectedErrands()));
		}

		final var startedBy = startedBy();
		final var startedByAdAccount = nonNull(getAdUser());

		return jobService.launch(new JobSpec(namespace, municipalityId, RESTRUCTURE_LABEL_TREE, affectedErrandIds.size(), null), labelRestructureTaskExecutor,
			jobId -> new LabelRestructureRun(jobId, namespace, municipalityId, request.getSteps(), affectedErrandIds, startedBy, startedByAdAccount),
			labelRestructureRunner::run,
			COULD_NOT_START_RESTRUCTURE);
	}

	/**
	 * Walks {@code steps} in order against a {@link LabelTreeSnapshot} built once from the namespace's current label
	 * tree, so that a step referencing a label an earlier step in this same list added or moved resolves against what
	 * that earlier step did, not against what is actually persisted yet. Used both for a pure dry-run
	 * ({@link #restructureLabelTree}) and as {@link #startLabelTreeRestructure}'s up-front validation pass - neither
	 * call persists anything itself, that is {@code LabelRestructureRunner}'s job once a run is actually dispatched.
	 */
	private StepSimulation simulateSteps(final String namespace, final String municipalityId, final List<LabelRestructureStep> steps) {
		final var snapshot = LabelTreeSnapshot.of(metadataLabelRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId));
		// Fetched once up front, rather than once per MOVE/MERGE step - every step in the same request asks the same
		// namespace the same question, and fetched at all only when some step actually needs it.
		final var needsActions = steps.stream().anyMatch(step -> (step.getType() == MOVE) || (step.getType() == MERGE));
		final var actions = needsActions ? actionConfigRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId) : List.<ActionConfigEntity>of();
		final var results = new ArrayList<LabelRestructureStepResult>();
		final var touchedLabelIds = new HashSet<String>();

		for (var index = 0; index < steps.size(); index++) {
			results.add(simulateStep(snapshot, actions, steps.get(index), index, touchedLabelIds));
		}

		return new StepSimulation(results, touchedLabelIds);
	}

	/**
	 * {@link #simulateSteps}' own result: the per-step results every caller wants, and the union of every real
	 * (persisted) label id a MOVE/MERGE step in this simulation touched - read only by
	 * {@link #startLabelTreeRestructure}, which needs the deduplicated affected-errand set that union resolves to,
	 * not the per-step sum {@link #restructureLabelTree} reports.
	 */
	private record StepSimulation(List<LabelRestructureStepResult> results, Set<String> touchedLabelIds) {
	}

	private LabelRestructureStepResult simulateStep(final LabelTreeSnapshot snapshot, final List<ActionConfigEntity> actions, final LabelRestructureStep step, final int index, final Set<String> touchedLabelIds) {
		return switch (step.getType()) {
			case ADD -> simulateAdd(snapshot, step, index);
			case RENAME -> simulateRename(snapshot, step, index);
			case DELETE -> simulateDelete(snapshot, step, index);
			case MOVE -> simulateMove(snapshot, actions, step, index, touchedLabelIds);
			case MERGE -> simulateMerge(snapshot, actions, step, index, touchedLabelIds);
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

	private LabelRestructureStepResult simulateMove(final LabelTreeSnapshot snapshot, final List<ActionConfigEntity> actions, final LabelRestructureStep step, final int index, final Set<String> touchedLabelIds) {
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
		touchedLabelIds.addAll(realIds);

		snapshot.recordMove(sourcePath, newPath);
		return stepResult(step, index, affected.errandCount(), affected.actions());
	}

	/**
	 * A descendant's resulting path can collide or overflow just as easily as the moved label's own, since both land
	 * under a destination neither of them has occupied before - checked here the same way
	 * {@link #validateNoDescendantPathCollision}/{@link #validateResourcePathLength} check it for
	 * {@link #revalidateAndReparent}'s own execution-time re-check, so a dry run and the up-front validation
	 * {@link #startLabelTreeRestructure} runs before dispatching a runner cannot report success on a step that would
	 * later fail mid-run against the DB's own path constraints.
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

	private LabelRestructureStepResult simulateMerge(final LabelTreeSnapshot snapshot, final List<ActionConfigEntity> actions, final LabelRestructureStep step, final int index, final Set<String> touchedLabelIds) {
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
		touchedLabelIds.addAll(realSourceIds);

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
		return new AffectedLabels(errandsRepository.findDistinctIdsByLabelsMetadataLabelIdIn(labelIds).size(), actionsReferencing(actions, labelIds));
	}

	private record AffectedLabels(long errandCount, List<AffectedAction> actions) {
	}

	/**
	 * Shared by {@link #simulateMove}/{@link #simulateMerge} - both need "which of this namespace's actions have a
	 * hasLabel condition naming any of these ids", asked via {@link #referencesAnyLabel}. Filters {@code actions}
	 * rather than querying the namespace itself, since {@link #simulateSteps} already fetched it once for the whole
	 * request - a restructure with several MOVE/MERGE steps must not re-run that same namespace-wide query once per
	 * step.
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

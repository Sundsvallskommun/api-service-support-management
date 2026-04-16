package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.Labels;
import se.sundsvall.supportmanagement.api.model.metadata.MetadataResponse;
import se.sundsvall.supportmanagement.api.model.metadata.Phase;
import se.sundsvall.supportmanagement.api.model.metadata.PhaseTransition;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.integration.db.CategoryRepository;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.ExternalIdTypeRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.PhaseRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;
import se.sundsvall.supportmanagement.integration.db.StatusRepository;
import se.sundsvall.supportmanagement.integration.db.ValidationRepository;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.ValidationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;
import se.sundsvall.supportmanagement.service.mapper.MetadataMapper;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.CollectionUtils.isEmpty;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toCategory;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toCategoryEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toContactReason;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toContactReasonEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toExternalIdType;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toExternalIdTypeEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toLabels;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toMetadataLabelEntityList;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toPhase;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toPhaseEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toPhaseTransitionEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toRole;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toRoleEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toStatus;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toStatusEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateContactReason;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateExternalIdTypeEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateMetadataLabelEntities;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updatePhaseEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateRoleEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateStatusEntity;

@Service
public class MetadataService {

	private static final String ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID = "%s '%s' already exists in namespace '%s' for municipalityId '%s'";
	private static final String ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID = "%s '%s' is not present in namespace '%s' for municipalityId '%s'";

	private static final String CONTACT_REASON = "ContactReason";
	private static final String CATEGORY = "Category";
	private static final String EXTERNAL_ID_TYPE = "ExternalIdType";
	private static final String PHASE = "Phase";
	private static final String PHASE_TRANSITION = "PhaseTransition";
	private static final String ROLE = "Role";
	private static final String STATUS = "Status";

	private final CategoryRepository categoryRepository;
	private final ErrandsRepository errandsRepository;
	private final ExternalIdTypeRepository externalIdTypeRepository;
	private final MetadataLabelRepository metadataLabelRepository;
	private final PhaseRepository phaseRepository;
	private final RoleRepository roleRepository;
	private final StatusRepository statusRepository;
	private final ValidationRepository validationRepository;
	private final ContactReasonRepository contactReasonRepository;
	private final AntPathMatcher pathMatcher;

	public MetadataService(final CategoryRepository categoryRepository,
		final ErrandsRepository errandsRepository,
		final ExternalIdTypeRepository externalIdTypeRepository,
		final MetadataLabelRepository metadataLabelRepository,
		final PhaseRepository phaseRepository,
		final RoleRepository roleRepository,
		final StatusRepository statusRepository,
		final ValidationRepository validationRepository,
		final ContactReasonRepository contactReasonRepository) {
		this.categoryRepository = categoryRepository;
		this.errandsRepository = errandsRepository;
		this.externalIdTypeRepository = externalIdTypeRepository;
		this.metadataLabelRepository = metadataLabelRepository;
		this.phaseRepository = phaseRepository;
		this.roleRepository = roleRepository;
		this.statusRepository = statusRepository;
		this.validationRepository = validationRepository;
		this.contactReasonRepository = contactReasonRepository;
		this.pathMatcher = new AntPathMatcher();
		this.pathMatcher.setCaseSensitive(false);
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
		return externalIdTypeRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, sort)
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
		return statusRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, sort)
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
		return roleRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, sort)
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

	public void deleteLabels(final String namespace, final String municipalityId) {
		if (!metadataLabelRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, "Labels are not present in namespace '%s' for municipalityId '%s'".formatted(namespace, municipalityId));
		}

		metadataLabelRepository.findByNamespaceAndMunicipalityIdAndParentIsNull(namespace, municipalityId).stream()
			.map(MetadataLabelEntity::getId)
			.forEach(metadataLabelRepository::deleteById);
	}

	@Transactional(readOnly = true)
	public Set<MetadataLabelEntity> patternToLabels(
		final String namespace,
		final String municipalityId,
		final List<String> resourcePathPatterns) {

		if (isEmpty(resourcePathPatterns)) {
			return Set.of();
		}

		final var potentialMatches = metadataLabelRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId);

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
		return categoryRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, sort)
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
		return contactReasonRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, sort).stream()
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
}

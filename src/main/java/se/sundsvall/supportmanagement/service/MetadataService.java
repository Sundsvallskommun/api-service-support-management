package se.sundsvall.supportmanagement.service;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toCategory;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toCategoryEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toContactReason;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toContactReasonEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toExternalIdType;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toExternalIdTypeEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toLabelEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toLabels;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toRole;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toRoleEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toStatus;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toStatusEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateContactReason;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateLabelEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.Labels;
import se.sundsvall.supportmanagement.api.model.metadata.MetadataResponse;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.integration.db.CategoryRepository;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ExternalIdTypeRepository;
import se.sundsvall.supportmanagement.integration.db.LabelRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;
import se.sundsvall.supportmanagement.integration.db.StatusRepository;
import se.sundsvall.supportmanagement.integration.db.ValidationRepository;
import se.sundsvall.supportmanagement.integration.db.model.ValidationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;
import se.sundsvall.supportmanagement.service.mapper.MetadataMapper;

@Service
public class MetadataService {

	private static final String CACHE_NAME = "metadataCache";
	private static final String ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID = "%s '%s' already exists in namespace '%s' for municipalityId '%s'";
	private static final String ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID = "%s '%s' is not present in namespace '%s' for municipalityId '%s'";

	private static final String CONTACT_REASON = "ContactReason";
	private static final String CATEGORY = "Category";
	private static final String EXTERNAL_ID_TYPE = "ExternalIdType";
	private static final String ROLE = "Role";
	private static final String STATUS = "Status";

	private final CategoryRepository categoryRepository;
	private final ExternalIdTypeRepository externalIdTypeRepository;
	private final LabelRepository labelRepository;
	private final RoleRepository roleRepository;
	private final StatusRepository statusRepository;
	private final ValidationRepository validationRepository;
	private final ContactReasonRepository contactReasonRepository;

	public MetadataService(final CategoryRepository categoryRepository,
		final ExternalIdTypeRepository externalIdTypeRepository,
		final LabelRepository labelRepository, final RoleRepository roleRepository,
		final StatusRepository statusRepository, final ValidationRepository validationRepository,
		final ContactReasonRepository contactReasonRepository) {
		this.categoryRepository = categoryRepository;
		this.externalIdTypeRepository = externalIdTypeRepository;
		this.labelRepository = labelRepository;
		this.roleRepository = roleRepository;
		this.statusRepository = statusRepository;
		this.validationRepository = validationRepository;
		this.contactReasonRepository = contactReasonRepository;
	}

	// =================================================================
	// Common operations
	// =================================================================

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public MetadataResponse findAll(final String namespace, final String municipalityId) {
		return MetadataResponse.create()
			.withCategories(findCategories(namespace, municipalityId))
			.withLabels(findLabels(namespace, municipalityId))
			.withStatuses(findStatuses(namespace, municipalityId))
			.withRoles(findRoles(namespace, municipalityId))
			.withExternalIdTypes(findExternalIdTypes(namespace, municipalityId))
			.withContactReasons(findContactReasons(namespace, municipalityId));
	}

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId, #type}")
	public boolean isValidated(final String namespace, final String municipalityId, final EntityType type) {
		return validationRepository.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)
			.map(ValidationEntity::isValidated)
			.orElse(false);
	}

	// =================================================================
	// ExternalIdType operations
	// =================================================================

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'findExternalIdTypes', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}")
	})
	public String createExternalIdType(final String namespace, final String municipalityId, final ExternalIdType externalIdType) {
		if (externalIdTypeRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, externalIdType.getName())) {
			throw Problem.valueOf(BAD_REQUEST, String.format(ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID, EXTERNAL_ID_TYPE, externalIdType.getName(), namespace, municipalityId));
		}

		return externalIdTypeRepository.save(toExternalIdTypeEntity(namespace, municipalityId, externalIdType)).getName();
	}

	public ExternalIdType getExternalIdType(final String namespace, final String municipalityId, final String name) {
		if (!externalIdTypeRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, EXTERNAL_ID_TYPE, name, namespace, municipalityId));
		}

		return toExternalIdType(externalIdTypeRepository.getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name));
	}

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public List<ExternalIdType> findExternalIdTypes(final String namespace, final String municipalityId) {
		return externalIdTypeRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.map(MetadataMapper::toExternalIdType)
			.filter(Objects::nonNull)
			.sorted(comparing(ExternalIdType::getName))
			.toList();
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'findExternalIdTypes', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}")
	})
	public void deleteExternalIdType(final String namespace, final String municipalityId, final String name) {
		if (!externalIdTypeRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, EXTERNAL_ID_TYPE, name, namespace, municipalityId));
		}

		externalIdTypeRepository.deleteByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
	}

	// =================================================================
	// Status operations
	// =================================================================

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'findStatuses', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}")
	})
	public String createStatus(final String namespace, final String municipalityId, final Status status) {
		if (statusRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, status.getName())) {
			throw Problem.valueOf(BAD_REQUEST, String.format(ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID, STATUS, status.getName(), namespace, municipalityId));
		}

		return statusRepository.save(toStatusEntity(namespace, municipalityId, status)).getName();
	}

	public Status getStatus(final String namespace, final String municipalityId, final String name) {
		if (!statusRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, STATUS, name, namespace, municipalityId));
		}

		return toStatus(statusRepository.getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name));
	}

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public List<Status> findStatuses(final String namespace, final String municipalityId) {
		return statusRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.map(MetadataMapper::toStatus)
			.filter(Objects::nonNull)
			.sorted(comparing(Status::getName))
			.toList();
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'findStatuses', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}")
	})
	public void deleteStatus(final String namespace, final String municipalityId, final String name) {
		if (!statusRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, STATUS, name, namespace, municipalityId));
		}

		statusRepository.deleteByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
	}

	// =================================================================
	// Role operations
	// =================================================================

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'findRoles', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}")
	})
	public String createRole(final String namespace, final String municipalityId, final Role role) {
		if (roleRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, role.getName())) {
			throw Problem.valueOf(BAD_REQUEST, String.format(ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID, ROLE, role.getName(), namespace, municipalityId));
		}

		return roleRepository.save(toRoleEntity(namespace, municipalityId, role)).getName();
	}

	public Role getRole(final String namespace, final String municipalityId, final String name) {
		if (!roleRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, ROLE, name, namespace, municipalityId));
		}

		return toRole(roleRepository.getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name));
	}

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public List<Role> findRoles(final String namespace, final String municipalityId) {
		return roleRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.map(MetadataMapper::toRole)
			.filter(Objects::nonNull)
			.sorted(comparing(Role::getName))
			.toList();
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'findRoles', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}")
	})
	public void deleteRole(final String namespace, final String municipalityId, final String name) {
		if (!roleRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, ROLE, name, namespace, municipalityId));
		}

		roleRepository.deleteByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
	}

	// =================================================================
	// Label operations
	// =================================================================

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'findLabels', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}")
	})
	public void createLabels(final String namespace, final String municipalityId, final List<Label> labels) {
		if (labelRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)) {
			throw Problem.valueOf(BAD_REQUEST, String.format("Labels already exists in namespace '%s' for municipalityId '%s'", namespace, municipalityId));
		}
		verifyUniqueNames(labels, new HashSet<>());
		labelRepository.save(toLabelEntity(namespace, municipalityId, labels));
	}

	@Caching(evict = {
			@CacheEvict(value = CACHE_NAME, key = "{'findLabels', #namespace, #municipalityId}"),
			@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}")
	})
	public void updateLabels(final String namespace, final String municipalityId, final List<Label> labels) {
		if (!labelRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format("Labels dos not exists in namespace '%s' for municipalityId '%s'", namespace, municipalityId));
		}
		verifyUniqueNames(labels, new HashSet<>());
		var entity = labelRepository.findOneByNamespaceAndMunicipalityId(namespace, municipalityId);
		labelRepository.save(updateLabelEntity(entity, labels));
	}

	private void verifyUniqueNames(final List<Label> labels, final Set<String> names) {
		if(labels == null) {
			return;
		}
		for(Label label : labels) {
			if(names.contains(label.getName())) {
				throw Problem.valueOf(BAD_REQUEST, String.format("Label names must be unique. Duplication detected for '%s'", label.getName()));
			}
			names.add(label.getName());
			verifyUniqueNames(label.getLabels(), names);
		}
	}

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public Labels findLabels(final String namespace, final String municipalityId) {
		return toLabels(labelRepository.findOneByNamespaceAndMunicipalityId(namespace, municipalityId));
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'findLabels', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}")
	})
	public void deleteLabels(final String namespace, final String municipalityId) {
		if (!labelRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format("Labels are not present in namespace '%s' for municipalityId '%s'", namespace, municipalityId));
		}
		labelRepository.deleteByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

	// =================================================================
	// Category and Type operations
	// =================================================================

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'findCategories', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findTypes', #namespace, #municipalityId}")
	})
	public String createCategory(final String namespace, final String municipalityId, final Category category) {
		if (categoryRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, category.getName())) {
			throw Problem.valueOf(BAD_REQUEST, String.format(ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID, CATEGORY, category.getName(), namespace, municipalityId));
		}

		return categoryRepository.save(toCategoryEntity(namespace, municipalityId, category)).getName();
	}

	public Category getCategory(final String namespace, final String municipalityId, final String name) {
		if (!categoryRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, CATEGORY, name, namespace, municipalityId));
		}

		return MetadataMapper.toCategory(categoryRepository.getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name));
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'findCategories', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findTypes', #namespace, #municipalityId}")
	})
	public Category updateCategory(final String namespace, final String municipalityId, final String name, final Category category) {
		if (!categoryRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, CATEGORY, name, namespace, municipalityId));
		}
		final var entity = updateEntity(categoryRepository.getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name), category);
		return toCategory(categoryRepository.save(entity));
	}

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public List<Category> findCategories(final String namespace, final String municipalityId) {
		return categoryRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.map(MetadataMapper::toCategory)
			.filter(Objects::nonNull)
			.sorted(comparing(Category::getDisplayName, nullsFirst(naturalOrder())))
			.toList();
	}

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId, #category}")
	public List<Type> findTypes(final String namespace, final String municipalityId, final String category) {
		return findCategories(namespace, municipalityId)
			.stream()
			.filter(entry -> Objects.equals(category, entry.getName()))
			.map(Category::getTypes)
			.findAny()
			.orElse(emptyList())
			.stream()
			.sorted(comparing(Type::getDisplayName, nullsFirst(naturalOrder())))
			.toList();
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'findCategories', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findTypes', #namespace, #municipalityId}")
	})
	public void deleteCategory(final String namespace, final String municipalityId, final String name) {
		if (!categoryRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, CATEGORY, name, namespace, municipalityId));
		}

		categoryRepository.deleteByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
	}


	// =================================================================
	// ContactReason Operations
	// =================================================================

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public List<ContactReason> findContactReasons(final String namespace, final String municipalityId) {
		return contactReasonRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId).stream()
			.map(MetadataMapper::toContactReason)
			.filter(Objects::nonNull)
			.toList();
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'findContactReasons', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}")
	})
	public String createContactReason(final String namespace, final String municipalityId, final ContactReason contactReason) {
		if (contactReasonRepository.existsByReasonIgnoreCaseAndNamespaceAndMunicipalityId(contactReason.getReason(), namespace, municipalityId)) {
			throw Problem.valueOf(BAD_REQUEST, String.format(ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID, CONTACT_REASON, contactReason.getReason(), namespace, municipalityId));
		}

		return contactReasonRepository.save(toContactReasonEntity(namespace, municipalityId, contactReason)).getReason();
	}

	public ContactReason getContactReasonByReasonAndNamespaceAndMunicipalityId(final String contactReason, final String namespace, final String municipalityId) {
		var contactReasonEntity = contactReasonRepository.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(contactReason, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, CONTACT_REASON, contactReason, namespace, municipalityId)));
		return toContactReason(contactReasonEntity);
	}

	public List<ContactReason> getContactReasonsForNamespaceAndMunicipality(final String namespace, final String municipalityId) {
		return contactReasonRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId).stream()
			.map(MetadataMapper::toContactReason)
			.toList();
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'findContactReasons', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}")
	})
	public ContactReason patchContactReason(final String reason, final String namespace, final String municipalityId, final ContactReason contactReason) {
		if (!contactReasonRepository.existsByReasonIgnoreCaseAndNamespaceAndMunicipalityId(reason, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, CONTACT_REASON, reason, namespace, municipalityId));
		}
		final var contactReasonEntity = updateContactReason(contactReasonRepository.getByReasonIgnoreCaseAndNamespaceAndMunicipalityId(reason, namespace, municipalityId), contactReason);

		return toContactReason(contactReasonRepository.save(contactReasonEntity));
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'findContactReasons', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}")
	})
	@Transactional
	public void deleteContactReason(final String contactReason, final String namespace, final String municipalityId) {
		if (!contactReasonRepository.existsByReasonIgnoreCaseAndNamespaceAndMunicipalityId(contactReason, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, CONTACT_REASON, contactReason, namespace, municipalityId));
		}
		contactReasonRepository.deleteByReasonIgnoreCaseAndNamespaceAndMunicipalityId(contactReason, namespace, municipalityId);
	}
}

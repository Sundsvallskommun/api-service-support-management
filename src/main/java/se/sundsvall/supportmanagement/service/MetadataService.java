package se.sundsvall.supportmanagement.service;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toExternalIdType;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toExternalIdTypeEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toRole;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toRoleEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toStatus;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toStatusEntity;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.MetadataResponse;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.integration.db.CategoryRepository;
import se.sundsvall.supportmanagement.integration.db.ExternalIdTypeRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;
import se.sundsvall.supportmanagement.integration.db.StatusRepository;
import se.sundsvall.supportmanagement.integration.db.ValidationRepository;
import se.sundsvall.supportmanagement.integration.db.model.ValidationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;
import se.sundsvall.supportmanagement.service.mapper.MetadataMapper;

@Service
public class MetadataService {

	private static final String EXTERNAL_ID_TYPE = ExternalIdType.class.getSimpleName();
	private static final String STATUS = Status.class.getSimpleName();
	private static final String ROLE = Role.class.getSimpleName();

	private static final String CACHE_NAME = "metadataCache";
	private static final String ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID = "%s '%s' already exists in namespace '%s' for municipalityId '%s'";
	private static final String ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID = "%s '%s' is not present in namespace '%s' for municipalityId '%s'";

	@Autowired
	private StatusRepository statusRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ExternalIdTypeRepository externalIdTypeRepository;

	@Autowired
	private ValidationRepository validationRepository;

	// =================================================================
	// Common operations
	// =================================================================

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public MetadataResponse findAll(final String namespace, final String municipalityId) {
		return MetadataResponse.create()
			.withCategories(findCategories(namespace, municipalityId))
			.withStatuses(findStatuses(namespace, municipalityId))
			.withRoles(findRoles(namespace, municipalityId))
			.withExternalIdTypes(findExternalIdTypes(namespace, municipalityId));
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
	// Category and Type operations
	// =================================================================

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
}

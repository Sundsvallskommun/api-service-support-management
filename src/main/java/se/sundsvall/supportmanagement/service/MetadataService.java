package se.sundsvall.supportmanagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.MetadataResponse;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.integration.db.CategoryRepository;
import se.sundsvall.supportmanagement.integration.db.ExternalIdTypeRepository;
import se.sundsvall.supportmanagement.integration.db.StatusRepository;
import se.sundsvall.supportmanagement.integration.db.ValidationRepository;
import se.sundsvall.supportmanagement.integration.db.model.ValidationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;
import se.sundsvall.supportmanagement.service.mapper.MetadataMapper;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toCategory;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toCategoryEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toExternalIdTypeEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toStatusEntity;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.updateEntity;

@Service
public class MetadataService {
	private static final String CACHE_NAME = "metadataCache";
	private static final String ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID = "%s '%s' already exists in namespace '%s' for municipalityId '%s'";
	private static final String ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID = "%s '%s' is not present in namespace '%s' for municipalityId '%s'";

	private static final String CATEGORY = "Category";
	private static final String STATUS = "Status";
	private static final String EXTERNAL_ID_TYPE = "ExternalIdType";

	@Autowired
	private StatusRepository statusRepository;

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
	public MetadataResponse findAll(String namespace, String municipalityId) {
		return MetadataResponse.create()
			.withCategories(findCategories(namespace, municipalityId))
			.withStatuses(findStatuses(namespace, municipalityId))
			.withExternalIdTypes(findExternalIdTypes(namespace, municipalityId));
	}

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId, #type}")
	public boolean isValidated(String namespace, String municipalityId, EntityType type) {
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
	public String createExternalIdType(String namespace, String municipalityId, ExternalIdType externalIdType) {
		if (externalIdTypeRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, externalIdType.getName())) {
			throw Problem.valueOf(BAD_REQUEST, String.format(ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID, EXTERNAL_ID_TYPE, externalIdType.getName(), namespace, municipalityId));
		}

		return externalIdTypeRepository.save(toExternalIdTypeEntity(namespace, municipalityId, externalIdType)).getName();
	}

	public ExternalIdType getExternalIdType(String namespace, String municipalityId, String name) {
		if (!externalIdTypeRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, EXTERNAL_ID_TYPE, name, namespace, municipalityId));
		}

		return MetadataMapper.toExternalIdType(externalIdTypeRepository.getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name));
	}

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public List<ExternalIdType> findExternalIdTypes(String namespace, String municipalityId) {
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
	public void deleteExternalIdType(String namespace, String municipalityId, String name) {
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
	public String createStatus(String namespace, String municipalityId, Status status) {
		if (statusRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, status.getName())) {
			throw Problem.valueOf(BAD_REQUEST, String.format(ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID, STATUS, status.getName(), namespace, municipalityId));
		}

		return statusRepository.save(toStatusEntity(namespace, municipalityId, status)).getName();
	}

	public Status getStatus(String namespace, String municipalityId, String name) {
		if (!statusRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, STATUS, name, namespace, municipalityId));
		}

		return MetadataMapper.toStatus(statusRepository.getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name));
	}

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public List<Status> findStatuses(String namespace, String municipalityId) {
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
	public void deleteStatus(String namespace, String municipalityId, String name) {
		if (!statusRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, STATUS, name, namespace, municipalityId));
		}

		statusRepository.deleteByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
	}

	// =================================================================
	// Category and Type operations
	// =================================================================

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'findCategories', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findTypes', #namespace, #municipalityId}")
	})
	public String createCategory(String namespace, String municipalityId, Category category) {
		if (categoryRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, category.getName())) {
			throw Problem.valueOf(BAD_REQUEST, String.format(ITEM_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID, CATEGORY, category.getName(), namespace, municipalityId));
		}

		return categoryRepository.save(toCategoryEntity(namespace, municipalityId, category)).getName();
	}

	public Category getCategory(String namespace, String municipalityId, String name) {
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
	public Category updateCategory(String namespace, String municipalityId, String name, Category category) {
		if (!categoryRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, CATEGORY, name, namespace, municipalityId));
		}
		final var entity = updateEntity(categoryRepository.getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name), category);
		return toCategory(categoryRepository.save(entity));
	}

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public List<Category> findCategories(String namespace, String municipalityId) {
		return categoryRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.map(MetadataMapper::toCategory)
			.filter(Objects::nonNull)
			.sorted(comparing(Category::getDisplayName, nullsFirst(naturalOrder())))
			.toList();
	}

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId, #category}")
	public List<Type> findTypes(String namespace, String municipalityId, String category) {
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
	public void deleteCategory(String namespace, String municipalityId, String name) {
		if (!categoryRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ITEM_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, CATEGORY, name, namespace, municipalityId));
		}

		categoryRepository.deleteByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
	}
}

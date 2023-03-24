package se.sundsvall.supportmanagement.service;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.MetadataMapper.toStatusEntity;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

@Service
public class MetadataService {
	private static final String CACHE_NAME = "metadataCache";
	private static final String STATUS_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID = "Status '%s' already exists in namespace '%s' for municipalityId '%s'";
	private static final String STATUS_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID = "Status '%s' is not present in namespace '%s' for municipalityId '%s'";

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

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public List<ExternalIdType> findExternalIdTypes(String namespace, String municipalityId) {
		return externalIdTypeRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.map(MetadataMapper::toExternalIdType)
			.filter(Objects::nonNull)
			.sorted(comparing(ExternalIdType::getName))
			.toList();
	}

	// =================================================================
	// Status operations
	// =================================================================

	@CacheEvict(value = CACHE_NAME, key = "{'findStatuses', #namespace, #municipalityId}")
	public String createStatus(String namespace, String municipalityId, Status status) {
		if (statusRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, status.getName())) {
			throw Problem.valueOf(BAD_REQUEST, String.format(STATUS_ALREADY_EXISTS_IN_NAMESPACE_FOR_MUNICIPALITY_ID, status.getName(), namespace, municipalityId));
		}

		return statusRepository.save(toStatusEntity(namespace, municipalityId, status)).getName();
	}

	public Status getStatus(String namespace, String municipalityId, String name) {
		if (!statusRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(STATUS_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, name, namespace, municipalityId));
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

	@CacheEvict(value = CACHE_NAME, key = "{'findStatuses', #namespace, #municipalityId}")
	public void deleteStatus(String namespace, String municipalityId, String name) {
		if (!statusRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, String.format(STATUS_NOT_PRESENT_IN_NAMESPACE_FOR_MUNICIPALITY_ID, name, namespace, municipalityId));
		}

		statusRepository.deleteByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);
	}

	// =================================================================
	// Category and Type operations
	// =================================================================

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
}

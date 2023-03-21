package se.sundsvall.supportmanagement.service;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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

	@Autowired
	private StatusRepository statusRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ExternalIdTypeRepository externalIdTypeRepository;

	@Autowired
	private ValidationRepository validationRepository;

	public MetadataResponse findAll(String namespace, String municipalityId) {
		return MetadataResponse.create()
			.withCategories(findCategories(namespace, municipalityId))
			.withStatuses(findStatuses(namespace, municipalityId))
			.withExternalIdTypes(findExternalIdTypes(namespace, municipalityId));
	}

	@Cacheable(value = "metadataCache", key = "{#root.methodName, #namespace, #municipalityId}")
	public List<ExternalIdType> findExternalIdTypes(String namespace, String municipalityId) {
		return externalIdTypeRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.map(MetadataMapper::toExternalIdType)
			.filter(Objects::nonNull)
			.toList();
	}

	@Cacheable(value = "metadataCache", key = "{#root.methodName, #namespace, #municipalityId}")
	public List<Status> findStatuses(String namespace, String municipalityId) {
		return statusRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.map(MetadataMapper::toStatus)
			.filter(Objects::nonNull)
			.toList();
	}

	@Cacheable(value = "metadataCache", key = "{#root.methodName, #namespace, #municipalityId}")
	public List<Category> findCategories(String namespace, String municipalityId) {
		return categoryRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.map(MetadataMapper::toCategory)
			.filter(Objects::nonNull)
			.sorted((o1, o2) -> ObjectUtils.compare(o1.getDisplayName(), o2.getDisplayName()))
			.toList();
	}

	@Cacheable(value = "metadataCache", key = "{#root.methodName, #namespace, #municipalityId, #category}")
	public List<Type> findTypes(String namespace, String municipalityId, String category) {
		return findCategories(namespace, municipalityId)
			.stream()
			.filter(entry -> Objects.equals(category, entry.getName()))
			.map(Category::getTypes)
			.findAny()
			.orElse(emptyList());
	}

	@Cacheable(value = "metadataCache", key = "{#root.methodName, #namespace, #municipalityId, #type}")
	public boolean isValidated(String namespace, String municipalityId, EntityType type) {
		return validationRepository.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)
			.map(ValidationEntity::isValidated)
			.orElse(false);
	}
}

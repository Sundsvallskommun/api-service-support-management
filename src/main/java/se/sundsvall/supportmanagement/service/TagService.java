package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import se.sundsvall.supportmanagement.api.model.tag.TagsResponse;
import se.sundsvall.supportmanagement.integration.db.CategoryRepository;
import se.sundsvall.supportmanagement.integration.db.ExternalIdTypeRepository;
import se.sundsvall.supportmanagement.integration.db.StatusRepository;
import se.sundsvall.supportmanagement.integration.db.ValidationRepository;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.db.model.ValidationEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;

@Service
public class TagService {

	@Autowired
	private StatusRepository statusRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ExternalIdTypeRepository externalIdTypeRepository;

	@Autowired
	private ValidationRepository validationRepository;

	@Cacheable(value = "tagCache", key = "{#root.methodName, #namespace, #municipalityId}")
	public TagsResponse findAllTags(String namespace, String municipalityId) {
		return TagsResponse.create()
			.withCategoryTags(findAllCategoryTags(namespace, municipalityId))
			.withStatusTags(findAllStatusTags(namespace, municipalityId))
			.withTypeTags(findAllTypeTags(namespace, municipalityId, "CATEGORY-1")); // TODO: Refactor when API is changed in UF-4537
	}

	@Cacheable(value = "tagCache", key = "{#root.methodName, #namespace, #municipalityId}")
	public List<String> findAllStatusTags(String namespace, String municipalityId) {
		return statusRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.map(StatusEntity::getName)
			.toList();
	}

	@Cacheable(value = "tagCache", key = "{#root.methodName, #namespace, #municipalityId}")
	public List<String> findAllExternalIdTypeTags(String namespace, String municipalityId) {
		return externalIdTypeRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.map(ExternalIdTypeEntity::getName)
			.toList();
	}

	@Cacheable(value = "tagCache", key = "{#root.methodName, #namespace, #municipalityId}")
	public List<String> findAllCategoryTags(String namespace, String municipalityId) {
		return categoryRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.map(CategoryEntity::getName)
			.toList();
	}

	@Cacheable(value = "tagCache", key = "{#root.methodName, #namespace, #municipalityId, #category}")
	public List<String> findAllTypeTags(String namespace, String municipalityId, String category) {
		return categoryRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.filter(entity -> Objects.equals(category, entity.getName()))
			.map(CategoryEntity::getTypes)
			.flatMap(List::stream)
			.map(TypeEntity::getName)
			.toList();
	}

	@Cacheable(value = "tagCache", key = "{#root.methodName, #namespace, #municipalityId, #type}")
	public boolean isValidated(String namespace, String municipalityId, EntityType type) {
		return validationRepository.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)
			.map(ValidationEntity::isValidated)
			.orElse(false);
	}
}

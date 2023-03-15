package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import se.sundsvall.supportmanagement.api.model.tag.TagsResponse;
import se.sundsvall.supportmanagement.integration.db.CategoryTagRepository;
import se.sundsvall.supportmanagement.integration.db.ExternalIdTypeTagRepository;
import se.sundsvall.supportmanagement.integration.db.StatusTagRepository;
import se.sundsvall.supportmanagement.integration.db.TagValidationRepository;
import se.sundsvall.supportmanagement.integration.db.model.CategoryTagEntity;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeTagEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusTagEntity;
import se.sundsvall.supportmanagement.integration.db.model.TagValidationEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeTagEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.TagType;

@Service
public class TagService {

	@Autowired
	private StatusTagRepository statusTagRepository;

	@Autowired
	private CategoryTagRepository categoryTagTagRepository;

	@Autowired
	private ExternalIdTypeTagRepository externalIdTypeTagRepository;

	@Autowired
	private TagValidationRepository tagValidationRepository;

	@Cacheable(value = "tagCache", key = "{#root.methodName, #namespace, #municipalityId}")
	public TagsResponse findAllTags(String namespace, String municipalityId) {
		return TagsResponse.create()
			.withCategoryTags(findAllCategoryTags(namespace, municipalityId))
			.withStatusTags(findAllStatusTags(namespace, municipalityId))
			.withTypeTags(findAllTypeTags(namespace, municipalityId, "CATEGORY-1")); // TODO: Refactor when API is changed in UF-4537
	}

	@Cacheable(value = "tagCache", key = "{#root.methodName, #namespace, #municipalityId}")
	public List<String> findAllStatusTags(String namespace, String municipalityId) {
		return statusTagRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.map(StatusTagEntity::getName)
			.toList();
	}

	@Cacheable(value = "tagCache", key = "{#root.methodName, #namespace, #municipalityId}")
	public List<String> findAllExternalIdTypeTags(String namespace, String municipalityId) {
		return externalIdTypeTagRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.map(ExternalIdTypeTagEntity::getName)
			.toList();
	}

	@Cacheable(value = "tagCache", key = "{#root.methodName, #namespace, #municipalityId}")
	public List<String> findAllCategoryTags(String namespace, String municipalityId) {
		return categoryTagTagRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.map(CategoryTagEntity::getName)
			.toList();
	}

	@Cacheable(value = "tagCache", key = "{#root.methodName, #namespace, #municipalityId, #category}")
	public List<String> findAllTypeTags(String namespace, String municipalityId, String category) {
		return categoryTagTagRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)
			.stream()
			.filter(entity -> Objects.equals(category, entity.getName()))
			.map(CategoryTagEntity::getTypeTags)
			.flatMap(List::stream)
			.map(TypeTagEntity::getName)
			.toList();
	}

	@Cacheable(value = "tagCache", key = "{#root.methodName, #namespace, #municipalityId, #type}")
	public boolean isValidated(String namespace, String municipalityId, TagType type) {
		return tagValidationRepository.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)
			.map(TagValidationEntity::isValidated)
			.orElse(false);
	}
}

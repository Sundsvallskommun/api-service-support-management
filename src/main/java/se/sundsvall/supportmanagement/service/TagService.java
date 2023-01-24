package se.sundsvall.supportmanagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import se.sundsvall.supportmanagement.api.model.tag.TagsResponse;
import se.sundsvall.supportmanagement.integration.db.TagRepository;
import se.sundsvall.supportmanagement.integration.db.model.TagType;

import java.util.List;

import static se.sundsvall.supportmanagement.integration.db.model.TagType.CATEGORY;
import static se.sundsvall.supportmanagement.integration.db.model.TagType.CLIENT_ID;
import static se.sundsvall.supportmanagement.integration.db.model.TagType.STATUS;
import static se.sundsvall.supportmanagement.integration.db.model.TagType.TYPE;
import static se.sundsvall.supportmanagement.service.mapper.TagMapper.toStringList;
import static se.sundsvall.supportmanagement.service.mapper.TagMapper.toTagsResponse;

@Service
public class TagService {

	@Autowired
	private TagRepository tagRepository;

	@Cacheable(value = "tagCache", key = "#root.methodName")
	public TagsResponse findAllTags() {
		return toTagsResponse(tagRepository.findAll());
	}

	@Cacheable(value = "tagCache", key = "#root.methodName")
	public List<String> findAllStatusTags() {
		return findAllTagsWithType(STATUS);
	}

	@Cacheable(value = "tagCache", key = "#root.methodName")
	public List<String> findAllCategoryTags() {
		return findAllTagsWithType(CATEGORY);
	}

	@Cacheable(value = "tagCache", key = "#root.methodName")
	public List<String> findAllTypeTags() {
		return findAllTagsWithType(TYPE);
	}

	@Cacheable(value = "tagCache", key = "#root.methodName")
	public List<String> findAllClientIdTags() {
		return findAllTagsWithType(CLIENT_ID);
	}

	private List<String> findAllTagsWithType(final TagType tagType) {
		return toStringList(tagRepository.findByType(tagType));
	}
}

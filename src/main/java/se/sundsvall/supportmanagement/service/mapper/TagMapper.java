package se.sundsvall.supportmanagement.service.mapper;

import se.sundsvall.supportmanagement.api.model.tag.TagsResponse;
import se.sundsvall.supportmanagement.integration.db.model.TagEntity;
import se.sundsvall.supportmanagement.integration.db.model.TagType;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static se.sundsvall.supportmanagement.integration.db.model.TagType.CATEGORY;
import static se.sundsvall.supportmanagement.integration.db.model.TagType.CLIENT_ID;
import static se.sundsvall.supportmanagement.integration.db.model.TagType.STATUS;
import static se.sundsvall.supportmanagement.integration.db.model.TagType.TYPE;

public class TagMapper {

	private TagMapper() {}

	public static List<String> toStringList(List<TagEntity> tagEntityList) {
		return Optional.ofNullable(tagEntityList).orElse(emptyList()).stream()
			.map(TagEntity::getName)
			.toList();
	}

	public static TagsResponse toTagsResponse(List<TagEntity> tagEntityList) {
		return TagsResponse.create()
			.withStatusTags(toStringListFilteredByType(tagEntityList, STATUS))
			.withCategoryTags(toStringListFilteredByType(tagEntityList, CATEGORY))
			.withTypeTags(toStringListFilteredByType(tagEntityList, TYPE))
			.withClientIdTags(toStringListFilteredByType(tagEntityList, CLIENT_ID));
	}

	private static List<String> toStringListFilteredByType(List<TagEntity> tagEntityList, TagType type) {
		return Optional.ofNullable(tagEntityList).orElse(emptyList()).stream()
			.filter(tagEntity -> type == tagEntity.getType())
			.map(TagEntity::getName)
			.toList();
	}
}

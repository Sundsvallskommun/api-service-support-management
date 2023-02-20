package se.sundsvall.supportmanagement.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import se.sundsvall.supportmanagement.integration.db.model.TagEntity;
import se.sundsvall.supportmanagement.integration.db.model.TagType;

class TagMapperTest {

	@Test
	void toStringList() {

		// Setup
		final var entityTagList = List.of(
			TagEntity.create().withName("Tag-1"),
			TagEntity.create().withName("Tag-2"),
			TagEntity.create().withName("Tag-3"),
			TagEntity.create().withName("Tag-4"),
			TagEntity.create().withName("Tag-5"));

		// Call
		final var tagStringList = TagMapper.toStringList(entityTagList);

		// Verification
		assertThat(tagStringList).containsExactly("Tag-1", "Tag-2", "Tag-3", "Tag-4", "Tag-5");
	}

	@Test
	void toStringListFromNull() {

		// Call
		final var tagStringList = TagMapper.toStringList(null);

		// Verification
		assertThat(tagStringList).isNotNull().isEmpty();
	}

	@Test
	void toTagsResponse() {

		// Setup
		final var entityTagList = List.of(
			TagEntity.create().withName("Category-1").withType(TagType.CATEGORY),
			TagEntity.create().withName("Category-2").withType(TagType.CATEGORY),
			TagEntity.create().withName("Status-1").withType(TagType.STATUS),
			TagEntity.create().withName("Status-2").withType(TagType.STATUS),
			TagEntity.create().withName("Type-1").withType(TagType.TYPE),
			TagEntity.create().withName("Type-2").withType(TagType.TYPE));

		// Call
		final var tagsResponse = TagMapper.toTagsResponse(entityTagList);

		// Verification
		assertThat(tagsResponse).isNotNull();
		assertThat(tagsResponse.getStatusTags()).containsExactly("Status-1", "Status-2");
		assertThat(tagsResponse.getCategoryTags()).containsExactly("Category-1", "Category-2");
		assertThat(tagsResponse.getTypeTags()).containsExactly("Type-1", "Type-2");
	}

	@Test
	void toTagsResponseFromNull() {

		// Call
		final var tagsResponse = TagMapper.toTagsResponse(null);

		// Verification
		assertThat(tagsResponse).isNotNull();
		assertThat(tagsResponse.getStatusTags()).isEmpty();
		assertThat(tagsResponse.getCategoryTags()).isEmpty();
		assertThat(tagsResponse.getTypeTags()).isEmpty();
	}
}

package se.sundsvall.supportmanagement.api.model.tag;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class TagsResponseTest {

	@Test
	void testBean() {
		assertThat(TagsResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {

		final var categoryTags = List.of("CATEGORY");
		final var statusTags = List.of("STATUS");
		final var typeTags = List.of("TYPE");

		final var tagsResponse = TagsResponse.create()
			.withCategoryTags(categoryTags)
			.withStatusTags(statusTags)
			.withTypeTags(typeTags);

		assertThat(tagsResponse.getCategoryTags()).isEqualTo(categoryTags);
		assertThat(tagsResponse.getStatusTags()).isEqualTo(statusTags);
		assertThat(tagsResponse.getTypeTags()).isEqualTo(typeTags);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(TagsResponse.create()).hasAllNullFieldsOrProperties();
		assertThat(new TagsResponse()).hasAllNullFieldsOrProperties();
	}
}

package se.sundsvall.supportmanagement.integration.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.TagEntity;
import se.sundsvall.supportmanagement.integration.db.model.TagType;

import java.time.OffsetDateTime;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static se.sundsvall.supportmanagement.integration.db.model.TagType.CATEGORY;
import static se.sundsvall.supportmanagement.integration.db.model.TagType.CLIENT_ID;
import static se.sundsvall.supportmanagement.integration.db.model.TagType.STATUS;

/**
 * Tag repository tests.
 *
 * @see src/test/resources/db/testdata.sql for data setup.
 */
@SpringBootTest
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class TagRepositoryTest {

	private static final long TEST_ID = 7L;
	private static final String TEST_NAME = "STATUS-1";
	private static final TagType TEST_TYPE = STATUS;

	@Autowired
	private TagRepository tagRepository;

	@Test
	void create() {

		final var tagName = "TestStatus";
		final var tagEntity = new TagEntity();
		tagEntity.setName(tagName);
		tagEntity.setType(TEST_TYPE);

		assertThat(tagRepository.findByNameIgnoreCase(tagEntity.getName())).isNotPresent();

		// Execution
		tagRepository.save(tagEntity);

		// Assertions
		final var persistedEntity = tagRepository.findByNameIgnoreCase(tagName);

		assertThat(persistedEntity).isPresent();
		assertThat(persistedEntity.get().getName()).isEqualTo(tagName);
		assertThat(persistedEntity.get().getType()).isEqualTo(TEST_TYPE);
		assertThat(persistedEntity.get().getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(persistedEntity.get().getUpdated()).isNull();
	}

	@Test
	void update() {

		// Setup
		final var existingTagEntity = tagRepository.findById(TEST_ID).orElseThrow();
		final var oldTagName = existingTagEntity.getName();
		final var newTagName = "CHANGED-NAME";

		// Execution
		existingTagEntity.setName(newTagName);
		existingTagEntity.setType(CATEGORY);
		tagRepository.save(existingTagEntity);

		// Assertions
		assertThat(tagRepository.findByNameIgnoreCase(oldTagName)).isNotPresent();
		final var updatedTag = tagRepository.findByNameIgnoreCase(newTagName);

		assertThat(updatedTag).isPresent();
		assertThat(updatedTag.get().getName()).isEqualTo(newTagName);
		assertThat(updatedTag.get().getType()).isEqualTo(CATEGORY);
		assertThat(updatedTag.get().getUpdated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
	}

	@Test
	void delete() {

		// Setup
		final var existingTagEntity = tagRepository.findById(TEST_ID).orElseThrow();

		// Execution
		tagRepository.delete(existingTagEntity);

		// Assertions
		assertThat(tagRepository.findById(TEST_ID)).isNotPresent();
	}

	@Test
	void findByName() {

		final var tagEntity = tagRepository.findByNameIgnoreCase(TEST_NAME);

		assertThat(tagEntity).isPresent();
		assertThat(tagEntity.get().getId()).isEqualTo(TEST_ID);
		assertThat(tagEntity.get().getName()).isEqualTo(TEST_NAME);
		assertThat(tagEntity.get().getType()).isEqualTo(TEST_TYPE);
	}

	@Test
	void findByNameDifferentCase() {

		// Execution
		final var tagEntity = tagRepository.findByNameIgnoreCase(TEST_NAME.toLowerCase());

		// Assertions
		assertThat(tagEntity).isPresent();
		assertThat(tagEntity.get().getId()).isEqualTo(TEST_ID);
		assertThat(tagEntity.get().getName()).isEqualTo(TEST_NAME);
		assertThat(tagEntity.get().getType()).isEqualTo(TEST_TYPE);
	}

	@Test
	void findByNameNotFound() {
		assertThat(tagRepository.findByNameIgnoreCase("thisNameDoesNotExist")).isNotPresent();
	}

	@Test
	void findByType() {

		final var tagEntities = tagRepository.findByType(CATEGORY);

		assertThat(tagEntities)
			.extracting(TagEntity::getId, TagEntity::getType, TagEntity::getName).containsExactlyInAnyOrder(
				tuple(1L, CATEGORY, "CATEGORY-1"),
				tuple(2L, CATEGORY, "CATEGORY-2"),
				tuple(3L, CATEGORY, "CATEGORY-3"));
	}

	@Test
	void findByTypeNull() {
		assertThat(tagRepository.findByType(null)).isEmpty();
	}

	@Test
	void findByTypeNotFound() {
		assertThat(tagRepository.findByType(CLIENT_ID)).isEmpty();
	}
}

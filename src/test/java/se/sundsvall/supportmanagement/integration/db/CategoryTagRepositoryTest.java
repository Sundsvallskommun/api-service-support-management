package se.sundsvall.supportmanagement.integration.db;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.transaction.Transactional;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.supportmanagement.integration.db.model.CategoryTagEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeTagEntity;

/**
 * CategoryTagRepository tests.
 *
 * @see src/test/resources/db/testdata.sql for data setup.
 */
@SpringBootTest
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class CategoryTagRepositoryTest {
	private static final long TEST_ID = 101;

	@Autowired
	private CategoryTagRepository categoryTagRepository;

	@Test
	@Transactional
	void create() {

		final var municipalityId = "municipalityId-1";
		final var name = "category-4";
		final var namespace = "namespace-1";
		final var displayName = "category-display-name-4";
		final var typeName = "type-1";
		final var typeDisplayName = "type-displayname-1";
		final var filter = CategoryTagEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace);
		final var entityToSave = CategoryTagEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace)
			.withDisplayName(displayName)
			.withTypeTags(List.of(TypeTagEntity.create().withName(typeName).withDisplayName(typeDisplayName)));

		// Assertions before execution
		assertThat(categoryTagRepository.findOne(Example.of(filter))).isEmpty();

		// Execution
		categoryTagRepository.save(entityToSave);

		// Assertions after execution
		final var persistedEntity = categoryTagRepository.findOne(Example.of(filter));

		assertThat(persistedEntity).isPresent();
		assertThat(persistedEntity.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(persistedEntity.get().getName()).isEqualTo(name);
		assertThat(persistedEntity.get().getDisplayName()).isEqualTo(displayName);
		assertThat(persistedEntity.get().getNamespace()).isEqualTo(namespace);
		assertThat(persistedEntity.get().getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(persistedEntity.get().getModified()).isNull();
		assertThat(persistedEntity.get().getTypeTags()).hasSize(1)
			.extracting(
				TypeTagEntity::getName,
				TypeTagEntity::getDisplayName)
			.containsExactly(
				tuple(typeName, typeDisplayName));
	}

	@Test
	@Transactional
	void update() {

		// Setup
		final var municipalityId = "municipalityId-1";
		final var name = "category-1";
		final var namespace = "namespace-1";
		final var filter = CategoryTagEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace);

		final var newTagName = "changed-name";
		final var newTypeTagName = "changed-type-name";
		final var newTypeTagDisplayName = "changed-type-display-name";
		final var existingEntity = categoryTagRepository.findOne(Example.of(filter)).orElseThrow();
		final var changedTypeList = List.of(TypeTagEntity.create().withName(newTypeTagName).withDisplayName(newTypeTagDisplayName).withCategoryTagEntity(existingEntity));

		// Execution
		existingEntity.withName(newTagName).withTypeTags(changedTypeList);
		categoryTagRepository.save(existingEntity);

		// Assertions
		assertThat(categoryTagRepository.findOne(Example.of(filter))).isNotPresent();

		final var updatedEntity = categoryTagRepository.findOne(Example.of(filter.withName(newTagName)));
		assertThat(updatedEntity).isPresent();
		assertThat(updatedEntity.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(updatedEntity.get().getName()).isEqualTo(newTagName);
		assertThat(updatedEntity.get().getNamespace()).isEqualTo(namespace);
		assertThat(updatedEntity.get().getTypeTags()).hasSize(1)
			.extracting(
				TypeTagEntity::getName,
				TypeTagEntity::getDisplayName)
			.containsExactly(
				tuple(newTypeTagName, newTypeTagDisplayName));
		assertThat(updatedEntity.get().getModified()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
	}

	@Test
	@Transactional
	void findAllByNamespaceAndMunicipalityId() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var namespace = "namespace-1";

		final var matches = categoryTagRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId);

		assertThat(matches).hasSize(3)
			.extracting(
				CategoryTagEntity::getDisplayName,
				CategoryTagEntity::getId,
				CategoryTagEntity::getMunicipalityId,
				CategoryTagEntity::getName,
				CategoryTagEntity::getNamespace)
			.containsExactlyInAnyOrder(
				tuple("category-display-name-1", 100L, municipalityId, "category-1", namespace),
				tuple("category-display-name-2", 101L, municipalityId, "category-2", namespace),
				tuple("category-display-name-3", 102L, municipalityId, "category-3", namespace));

		final var verifications = Map.of(
			"category-1", List.of(
				tuple("type-display-name-1", "escalation-email-1", 100L, "type-1"),
				tuple("type-display-name-2", "escalation-email-2", 101L, "type-2"),
				tuple("type-display-name-3", "escalation-email-3", 102L, "type-3")),
			"category-2", List.of(
				tuple("type-display-name-1", "escalation-email-1", 103L, "type-1"),
				tuple("type-display-name-2", "escalation-email-2", 104L, "type-2")),
			"category-3", List.of(
				tuple("type-display-name-1", "escalation-email-1", 105L, "type-1")));

		verifyTypes(matches, verifications);
	}

	private void verifyTypes(List<CategoryTagEntity> matches, Map<String, List<Tuple>> verifications) {
		verifications.entrySet().stream()
			.forEach(entry -> {
				assertThat(extractTypeTags(matches, entry))
					.hasSize(entry.getValue().size())
					.extracting(
						TypeTagEntity::getDisplayName,
						TypeTagEntity::getEscalationEmail,
						TypeTagEntity::getId,
						TypeTagEntity::getName)
					.containsExactlyInAnyOrderElementsOf(entry.getValue());
			});
	}

	private List<TypeTagEntity> extractTypeTags(List<CategoryTagEntity> matches, Entry<String, List<Tuple>> entry) {
		return matches.stream()
			.filter(ct -> entry.getKey().equals(ct.getName())).findAny()
			.map(CategoryTagEntity::getTypeTags)
			.orElseThrow();
	}

	@Test
	void delete() {

		// Setup
		final var existingTagEntity = categoryTagRepository.findById(TEST_ID).orElseThrow();

		// Execution
		categoryTagRepository.delete(existingTagEntity);

		// Assertions
		assertThat(categoryTagRepository.findById(TEST_ID)).isNotPresent();
	}
}

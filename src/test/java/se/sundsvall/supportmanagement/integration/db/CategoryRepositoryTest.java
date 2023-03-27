package se.sundsvall.supportmanagement.integration.db;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeEntity;

import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * CategoryRepository tests.
 *
 * @see src/test/resources/db/testdata.sql for data setup.
 */
@SpringBootTest
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class CategoryRepositoryTest {
	private static final long TEST_ID = 101;

	@Autowired
	private CategoryRepository categoryRepository;

	@Test
	@Transactional
	void create() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var name = "category-4";
		final var namespace = "namespace-1";
		final var displayName = "category-display-name-4";
		final var typeName = "type-1";
		final var typeDisplayName = "type-displayname-1";
		final var filter = CategoryEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace);
		final var entityToSave = CategoryEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace)
			.withDisplayName(displayName)
			.withTypes(List.of(TypeEntity.create().withName(typeName).withDisplayName(typeDisplayName)));

		// Assertions before execution
		assertThat(categoryRepository.findOne(Example.of(filter))).isEmpty();

		// Execution
		categoryRepository.save(entityToSave);

		// Assertions after execution
		final var persistedEntity = categoryRepository.findOne(Example.of(filter));

		assertThat(persistedEntity).isPresent();
		assertThat(persistedEntity.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(persistedEntity.get().getName()).isEqualTo(name);
		assertThat(persistedEntity.get().getDisplayName()).isEqualTo(displayName);
		assertThat(persistedEntity.get().getNamespace()).isEqualTo(namespace);
		assertThat(persistedEntity.get().getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(persistedEntity.get().getModified()).isNull();
		assertThat(persistedEntity.get().getTypes()).hasSize(1)
			.extracting(
				TypeEntity::getName,
				TypeEntity::getDisplayName)
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
		final var filter = CategoryEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace);

		final var newName = "changed-name";
		final var newTypeName = "changed-type-name";
		final var newTypeDisplayName = "changed-type-display-name";
		final var existingEntity = categoryRepository.findOne(Example.of(filter)).orElseThrow();
		final var changedTypeList = List.of(TypeEntity.create().withName(newTypeName).withDisplayName(newTypeDisplayName).withCategoryEntity(existingEntity));

		// Execution
		existingEntity.withName(newName).withTypes(changedTypeList);
		categoryRepository.save(existingEntity);

		// Assertions
		assertThat(categoryRepository.findOne(Example.of(filter))).isNotPresent();

		final var updatedEntity = categoryRepository.findOne(Example.of(filter.withName(newName)));
		assertThat(updatedEntity).isPresent();
		assertThat(updatedEntity.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(updatedEntity.get().getName()).isEqualTo(newName);
		assertThat(updatedEntity.get().getNamespace()).isEqualTo(namespace);
		assertThat(updatedEntity.get().getTypes()).hasSize(1)
			.extracting(
				TypeEntity::getName,
				TypeEntity::getDisplayName)
			.containsExactly(
				tuple(newTypeName, newTypeDisplayName));
		assertThat(updatedEntity.get().getModified()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
	}

	@Test
	@Transactional
	void findAllByNamespaceAndMunicipalityId() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var namespace = "namespace-1";

		final var matches = categoryRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId);

		assertThat(matches).hasSize(3)
			.extracting(
				CategoryEntity::getDisplayName,
				CategoryEntity::getId,
				CategoryEntity::getMunicipalityId,
				CategoryEntity::getName,
				CategoryEntity::getNamespace)
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

	@Test
	@Transactional
	void getCategoryByNamespaceAndMunicipalityIdAndName() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var namespace = "namespace-1";
		final var category = "category-1";

		final var categoryEntity = categoryRepository.getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, category);

		assertThat(categoryEntity).isNotNull();
		assertThat(categoryEntity.getDisplayName()).isEqualTo("category-display-name-1");
		assertThat(categoryEntity.getId()).isEqualTo(100L);
		assertThat(categoryEntity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(categoryEntity.getName()).isEqualTo(category);
		assertThat(categoryEntity.getNamespace()).isEqualTo(namespace);
	}

	@Test
	void existsByNamespaceAndMunicipalityIdAndName() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var namespace = "namespace-1";
		final var existing_categoryname = "category-3";
		final var nonexisting_categoryname = "category-4";

		// Execution & assertion
		assertThat(categoryRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, existing_categoryname)).isTrue();
		assertThat(categoryRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, nonexisting_categoryname)).isFalse();
	}

	private void verifyTypes(List<CategoryEntity> matches, Map<String, List<Tuple>> verifications) {
		verifications.entrySet()
			.forEach(entry -> {
				assertThat(extractTypes(matches, entry))
					.hasSize(entry.getValue().size())
					.extracting(
						TypeEntity::getDisplayName,
						TypeEntity::getEscalationEmail,
						TypeEntity::getId,
						TypeEntity::getName)
					.containsExactlyInAnyOrderElementsOf(entry.getValue());
			});
	}

	private List<TypeEntity> extractTypes(List<CategoryEntity> matches, Entry<String, List<Tuple>> entry) {
		return matches.stream()
			.filter(ct -> entry.getKey().equals(ct.getName())).findAny()
			.map(CategoryEntity::getTypes)
			.orElseThrow();
	}

	@Test
	void delete() {
		// Setup
		final var existingEntity = categoryRepository.findById(TEST_ID).orElseThrow();

		// Execution
		categoryRepository.deleteByNamespaceAndMunicipalityIdAndName(existingEntity.getNamespace(), existingEntity.getMunicipalityId(), existingEntity.getName());

		// Assertions
		assertThat(categoryRepository.findById(TEST_ID)).isNotPresent();
	}
}

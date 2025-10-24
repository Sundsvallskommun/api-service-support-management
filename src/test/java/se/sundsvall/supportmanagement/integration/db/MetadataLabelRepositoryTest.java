package se.sundsvall.supportmanagement.integration.db;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

/**
 * MetadataLabelRepository tests.
 *
 * @see <a href="file:src/test/resources/db/testdata-junit.sql">src/test/resources/db/testdata-junit.sql</a> for data
 *      setup.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class MetadataLabelRepositoryTest {

	@Autowired
	private MetadataLabelRepository metadataLabelRepository;

	@Test
	void create() {

		// Arrange
		final var municipalityId = "2289";
		final var namespace = "namespace-99";

		final var entity = MetadataLabelEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId);

		// Act
		final var persistedEntity = metadataLabelRepository.save(entity);

		// Assert
		assertThat(persistedEntity).isNotNull();
		assertThat(persistedEntity.getId()).isNotBlank();
		assertThat(persistedEntity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(persistedEntity.getNamespace()).isEqualTo(namespace);
		assertThat(persistedEntity.getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(persistedEntity.getModified()).isNull();
	}

	@Test
	void createWithDuplicateValues() {

		// Arrange
		final var namespace = "namespace-123";
		final var municipalityId = "2285";
		final var resourcePath = "parent";

		final var existingOptionalEntity = metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePath(namespace, municipalityId, resourcePath);
		assertThat(existingOptionalEntity).isPresent();

		final var entity = MetadataLabelEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withResourceName(resourcePath);

		// Act and assert that no duplicates of municipalityId, namespace and resourcePath can exist in table.
		assertThrows(DataIntegrityViolationException.class, () -> metadataLabelRepository.saveAndFlush(entity));
	}

	@Test
	void update() {

		// Arrange
		final var id = "821033d0-f059-4f2b-90f2-cc0562ac0560"; // see testdata-junit.sql
		final var newResourceName = "newResourceName";
		final var existingOptionalEntity = metadataLabelRepository.findById(id);
		assertThat(existingOptionalEntity).isPresent();
		final var existingEntity = existingOptionalEntity.get();

		// Act
		metadataLabelRepository.saveAndFlush(existingEntity.withResourceName(newResourceName));

		// Assert
		final var updatedOptionalEntity = metadataLabelRepository.findById(id);
		assertThat(updatedOptionalEntity).isPresent();
		final var updatedEntity = updatedOptionalEntity.get();
		assertThat(updatedEntity.getCreated()).isEqualTo(existingEntity.getCreated());
		assertThat(updatedEntity.getModified()).isCloseTo(now(), within(2, SECONDS));
		assertThat(updatedEntity.getResourceName()).isEqualTo(newResourceName);
	}

	@Test
	void delete() {

		// Setup
		final var id = "4bee7529-b904-4559-97ae-0437f74de935"; // see testdata-junit.sql

		// Verify existing label
		assertThat(metadataLabelRepository.existsById(id)).isTrue();

		// Act
		metadataLabelRepository.deleteById(id);

		// Assert
		assertThat(metadataLabelRepository.existsById(id)).isFalse();
	}

	@Test
	void createHierarchicalEntityStructureAndVerifyResourcePaths() {

		// Arrange
		final var municipalityId = "2289";
		final var namespace = "namespace-hierarchy-1";

		final var parent = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("parent");

		final var level1 = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("level1")
			.withParent(parent);

		final var level2a = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("level2a")
			.withParent(level1);

		final var level2b = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("level2b")
			.withParent(level1);

		final var level3 = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("level3")
			.withParent(level2a);

		// Link hierarchy
		parent.addChild(level1);
		level1.addChild(level2a);
		level1.addChild(level2b);
		level2a.addChild(level3);

		// Act
		metadataLabelRepository.saveAndFlush(parent);

		// Assert — check that all nodes have the correct resourcePath
		final var allEntities = metadataLabelRepository.findAll().stream()
			.filter(e -> namespace.equals(e.getNamespace()))
			.toList();

		assertThat(allEntities)
			.hasSize(5)
			.extracting(
				MetadataLabelEntity::getResourcePath,
				MetadataLabelEntity::getResourceName)
			.containsExactlyInAnyOrder(
				tuple("parent", "parent"),
				tuple("parent/level1", "level1"),
				tuple("parent/level1/level2a", "level2a"),
				tuple("parent/level1/level2b", "level2b"),
				tuple("parent/level1/level2a/level3", "level3"));

		// Act 2 — update the parents resourceName and save again
		parent.setResourceName("parent-renamed");
		metadataLabelRepository.saveAndFlush(parent);

		// Assert 2 — check that children's resourcePath is also updated recursively
		final var updatedEntities = metadataLabelRepository.findAll().stream()
			.filter(e -> namespace.equals(e.getNamespace()))
			.toList();

		assertThat(updatedEntities)
			.extracting(
				MetadataLabelEntity::getResourcePath,
				MetadataLabelEntity::getResourceName)
			.containsExactlyInAnyOrder(
				tuple("parent-renamed", "parent-renamed"),
				tuple("parent-renamed/level1", "level1"),
				tuple("parent-renamed/level1/level2a", "level2a"),
				tuple("parent-renamed/level1/level2b", "level2b"),
				tuple("parent-renamed/level1/level2a/level3", "level3"));
	}

	@Test
	void updateInTheMiddleOfTheHierarchicalEntityStructureAndVerifyResourcePaths() {

		// Arrange
		final var municipalityId = "2289";
		final var namespace = "namespace-hierarchy-2";

		final var parent = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("parent");

		final var level1 = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("level1")
			.withParent(parent);

		final var level2a = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("level2a")
			.withParent(level1);

		final var level2b = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("level2b")
			.withParent(level1);

		final var level3a = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("level3a")
			.withParent(level2a);

		final var level3b = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("level3b")
			.withParent(level2b);

		// Link hierarchy
		parent.addChild(level1);
		level1.addChild(level2a);
		level1.addChild(level2b);
		level2a.addChild(level3a);
		level2b.addChild(level3b);

		// Act
		metadataLabelRepository.saveAndFlush(parent);

		// Assert — check that all nodes have the correct resourcePath
		final var allEntities = metadataLabelRepository.findAll().stream()
			.filter(e -> namespace.equals(e.getNamespace()))
			.toList();

		assertThat(allEntities)
			.hasSize(6)
			.extracting(
				MetadataLabelEntity::getResourcePath,
				MetadataLabelEntity::getResourceName)
			.containsExactlyInAnyOrder(
				tuple("parent", "parent"),
				tuple("parent/level1", "level1"),
				tuple("parent/level1/level2a", "level2a"),
				tuple("parent/level1/level2b", "level2b"),
				tuple("parent/level1/level2a/level3a", "level3a"),
				tuple("parent/level1/level2b/level3b", "level3b"));

		// Act 2 — update the parents and middle node:s resourceName and save again. Save the middle node
		final var level2aEntity = metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePath(namespace, municipalityId, "parent/level1/level2a").get();
		level2aEntity.setResourceName("level2a-renamed");
		parent.setResourceName("parent-renamed");
		metadataLabelRepository.saveAndFlush(level2aEntity);

		// Assert 2 — check that children's resourcePath is also updated recursively
		final var updatedEntities = metadataLabelRepository.findAll().stream()
			.filter(e -> namespace.equals(e.getNamespace()))
			.toList();

		assertThat(updatedEntities)
			.extracting(
				MetadataLabelEntity::getResourcePath,
				MetadataLabelEntity::getResourceName)
			.containsExactlyInAnyOrder(
				tuple("parent-renamed", "parent-renamed"),
				tuple("parent-renamed/level1", "level1"),
				tuple("parent-renamed/level1/level2a-renamed", "level2a-renamed"),
				tuple("parent-renamed/level1/level2b", "level2b"),
				tuple("parent-renamed/level1/level2a-renamed/level3a", "level3a"),
				tuple("parent-renamed/level1/level2b/level3b", "level3b"));
	}

	@Test
	void updateWithNewChildAndVerifyResourcePaths() {

		// Arrange
		final var municipalityId = "2289";
		final var namespace = "namespace-hierarchy-3";

		final var parent = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("parent");

		final var level1 = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("level1")
			.withParent(parent);

		final var level2a = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("level2a")
			.withParent(level1);

		final var level2b = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("level2b")
			.withParent(level1);

		final var level3a = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("level3a")
			.withParent(level2a);

		final var level3b = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("level3b")
			.withParent(level2b);

		// Link hierarchy
		parent.addChild(level1);
		level1.addChild(level2a);
		level1.addChild(level2b);
		level2a.addChild(level3a);
		level2b.addChild(level3b);

		// Act
		metadataLabelRepository.saveAndFlush(parent);

		// Assert — check that all nodes have the correct resourcePath
		final var allEntities = metadataLabelRepository.findAll().stream()
			.filter(e -> namespace.equals(e.getNamespace()))
			.toList();

		assertThat(allEntities)
			.hasSize(6)
			.extracting(
				MetadataLabelEntity::getResourcePath,
				MetadataLabelEntity::getResourceName)
			.containsExactlyInAnyOrder(
				tuple("parent", "parent"),
				tuple("parent/level1", "level1"),
				tuple("parent/level1/level2a", "level2a"),
				tuple("parent/level1/level2b", "level2b"),
				tuple("parent/level1/level2a/level3a", "level3a"),
				tuple("parent/level1/level2b/level3b", "level3b"));

		// Act 2 — update with a new child node
		final var level2cEntity = MetadataLabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withResourceName("level2c")
			.withParent(level1);

		metadataLabelRepository.saveAndFlush(level2cEntity);

		// Assert 2 — check that children's resourcePath is also updated recursively
		final var updatedEntities = metadataLabelRepository.findAll().stream()
			.filter(e -> namespace.equals(e.getNamespace()))
			.toList();

		assertThat(updatedEntities)
			.extracting(
				MetadataLabelEntity::getResourcePath,
				MetadataLabelEntity::getResourceName)
			.containsExactlyInAnyOrder(
				tuple("parent", "parent"),
				tuple("parent/level1", "level1"),
				tuple("parent/level1/level2a", "level2a"),
				tuple("parent/level1/level2b", "level2b"),
				tuple("parent/level1/level2c", "level2c"),
				tuple("parent/level1/level2a/level3a", "level3a"),
				tuple("parent/level1/level2b/level3b", "level3b"));
	}

	@Test
	void findByNamespaceAndMunicipalityId() {

		assertThat(metadataLabelRepository.findByNamespaceAndMunicipalityId("namespace-1", "2281"))
			.extracting(MetadataLabelEntity::getId)
			// see testdata-junit.sql
			.containsExactlyInAnyOrder(
				"9b2a5a7d-3e8f-4a7c-8f9b-2f7d3c1a0b11",
				"f3a1c6d9-7b2e-4d5a-9c1f-8e2b2c3d4e5f",
				"a0bb7b61-8d55-4857-b619-547572eed26f",
				"86d459cd-4810-4b4a-b365-97aa0c2c0ff5");
	}
}

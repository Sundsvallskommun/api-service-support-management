package se.sundsvall.supportmanagement.integration.db;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
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
		final var municipalityId = "2282";
		final var namespace = "namespace-1";
		final var resourcePath = "parent/child/resource3";

		final var existingOptionalEntity = metadataLabelRepository.findByNamespaceAndMunicipalityIdAndResourcePath(namespace, municipalityId, resourcePath);
		assertThat(existingOptionalEntity).isPresent();

		final var entity = MetadataLabelEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withResourcePath(resourcePath);

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
		final var id = "821033d0-f059-4f2b-90f2-cc0562ac0560"; // see testdata-junit.sql

		// Verify existing label
		assertThat(metadataLabelRepository.existsById(id)).isTrue();

		// Act
		metadataLabelRepository.deleteById(id);

		// Assert
		assertThat(metadataLabelRepository.existsById(id)).isFalse();
	}
}

package se.sundsvall.supportmanagement.integration.db;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Example;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.LabelEntity;

/**
 * StatusRepository tests.
 *
 * @see <a href="file:src/test/resources/db/testdata.sql">src/test/resources/db/testdata.sql</a> for data setup.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class LabelRepositoryTest {
	private static final String JSON_STRUCTURE = "[{\"key\": \"value\"}]";

	@Autowired
	private LabelRepository labelRepository;

	@Test
	void create() {
		// Setup
		final var municipalityId = "2289";
		final var namespace = "namespace-99";

		final var entity = LabelEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withJsonStructure(JSON_STRUCTURE);

		assertThat(labelRepository.findOne(Example.of(entity))).isEmpty();

		// Act
		labelRepository.save(entity);

		// Assert
		final var persistedEntity = labelRepository.findOne(Example.of(entity));

		assertThat(persistedEntity).isPresent();
		assertThat(persistedEntity.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(persistedEntity.get().getNamespace()).isEqualTo(namespace);
		assertThat(persistedEntity.get().getJsonStructure()).isEqualTo(JSON_STRUCTURE);
		assertThat(persistedEntity.get().getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(persistedEntity.get().getModified()).isNull();
	}

	@Test
	void createWithDuplicateValues() {
		// Setup
		final var municipalityId = "2281";
		final var namespace = "namespace-1";

		final var entity = LabelEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withJsonStructure(JSON_STRUCTURE);

		// Act and verify that no duplicates of municipalityId and namespace can exist in table
		assertThrows(DataIntegrityViolationException.class, () -> labelRepository.save(entity));
	}

	@Test
	void update() {
		// Setup
		final var municipalityId = "2281";
		final var namespace = "namespace-1";
		final var entity = LabelEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withJsonStructure(JSON_STRUCTURE);

		final var existingEntity = labelRepository.findOne(Example.of(entity)).orElseThrow();

		// Act
		final var newJsonStructure = "[{\"key\": \"value\"}, {\"key\": \"other_value\"}]";
		labelRepository.save(existingEntity.withJsonStructure(newJsonStructure));

		// Assert
		final var updatedEntity = labelRepository.findOne(Example.of(entity.withJsonStructure(newJsonStructure)));
		assertThat(updatedEntity).isPresent();
		assertThat(updatedEntity.get().getCreated()).isEqualTo(existingEntity.getCreated());
		assertThat(updatedEntity.get().getModified()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(labelRepository.findOne(Example.of(entity.withJsonStructure(JSON_STRUCTURE)))).isNotPresent();
	}

	@Test
	void findOneByNamespaceAndMunicipalityId() {
		// Setup
		final var municipalityId = "2281";
		final var namespace = "namespace-1";

		// Act
		final var entity = labelRepository.findOneByNamespaceAndMunicipalityId(namespace, municipalityId);

		// Assert
		assertThat(entity.getId()).isEqualTo(1);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getJsonStructure()).isNotBlank();
	}

	@Test
	void delete() {
		// Setup
		final var municipalityId = "2282";
		final var namespace = "namespace-1";

		// Verify existing label
		assertThat(labelRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)).isTrue();

		// Act
		labelRepository.deleteByNamespaceAndMunicipalityId(namespace, municipalityId);

		// Assert
		assertThat(labelRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)).isFalse();
	}

	@Test
	void existsByNamespaceAndMunicipalityId() {
		// Setup
		final var municipalityIdWithLabels = "2281";
		final var municipalityIdWithoutLabels = "2283";
		final var namespaceWithLabels = "namespace-1";
		final var namespaceWithoutLabels = "namespace-3";

		assertThat(labelRepository.existsByNamespaceAndMunicipalityId(namespaceWithLabels, municipalityIdWithLabels)).isTrue();
		assertThat(labelRepository.existsByNamespaceAndMunicipalityId(namespaceWithLabels, municipalityIdWithoutLabels)).isFalse();
		assertThat(labelRepository.existsByNamespaceAndMunicipalityId(namespaceWithoutLabels, municipalityIdWithLabels)).isFalse();
		assertThat(labelRepository.existsByNamespaceAndMunicipalityId(namespaceWithoutLabels, municipalityIdWithoutLabels)).isFalse();
	}
}

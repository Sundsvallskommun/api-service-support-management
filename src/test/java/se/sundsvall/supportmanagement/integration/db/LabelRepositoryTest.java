package se.sundsvall.supportmanagement.integration.db;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Example;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.supportmanagement.integration.db.model.LabelEntity;

/**
 * StatusRepository tests.
 *
 * @see src/test/resources/db/testdata.sql for data setup.
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
		final var municipalityId = "municipalityId-99";
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
	void update() {
		// Setup
		final var municipalityId = "municipalityId-1";
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
		final var municipalityId = "municipalityId-1";
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
		// Verify existing label
		assertThat(labelRepository.existsById(2L)).isTrue();
		
		// Act
		labelRepository.deleteById(2L);

		// Assert
		assertThat(labelRepository.existsById(2L)).isFalse();

	}
}

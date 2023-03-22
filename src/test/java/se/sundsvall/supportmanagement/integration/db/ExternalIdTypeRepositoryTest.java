package se.sundsvall.supportmanagement.integration.db;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeEntity;

/**
 * ExternalIdTypeRepository tests.
 *
 * @see src/test/resources/db/testdata.sql for data setup.
 */
@SpringBootTest
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class ExternalIdTypeRepositoryTest {
	private static final long TEST_ID = 101;

	@Autowired
	private ExternalIdTypeRepository externalIdTypeRepository;

	@Test
	void create() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var name = "external-id-type-4";
		final var namespace = "namespace-1";
		final var entity = ExternalIdTypeEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace);

		assertThat(externalIdTypeRepository.findOne(Example.of(entity))).isEmpty();

		// Execution
		externalIdTypeRepository.save(entity);

		// Assertions
		final var persistedEntity = externalIdTypeRepository.findOne(Example.of(entity));

		assertThat(persistedEntity).isPresent();
		assertThat(persistedEntity.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(persistedEntity.get().getName()).isEqualTo(name);
		assertThat(persistedEntity.get().getNamespace()).isEqualTo(namespace);
		assertThat(persistedEntity.get().getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(persistedEntity.get().getModified()).isNull();
	}

	@Test
	void update() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var name = "external-id-type-1";
		final var namespace = "namespace-1";
		final var entity = ExternalIdTypeEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace);

		final var existingEntity = externalIdTypeRepository.findOne(Example.of(entity)).orElseThrow();
		final var newName = "changed-name";

		// Execution
		existingEntity.setName(newName);
		externalIdTypeRepository.save(existingEntity);

		// Assertions
		assertThat(externalIdTypeRepository.findOne(Example.of(entity))).isNotPresent();
		final var updatedEntity = externalIdTypeRepository.findOne(Example.of(entity.withName(newName)));

		assertThat(updatedEntity).isPresent();
		assertThat(updatedEntity.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(updatedEntity.get().getName()).isEqualTo(newName);
		assertThat(updatedEntity.get().getNamespace()).isEqualTo(namespace);
		assertThat(updatedEntity.get().getModified()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
	}

	@Test
	void findAllByNamespaceAndMunicipalityId() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var namespace = "namespace-1";

		final var matches = externalIdTypeRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId);

		assertThat(matches).hasSize(3)
			.extracting(
				ExternalIdTypeEntity::getMunicipalityId,
				ExternalIdTypeEntity::getName,
				ExternalIdTypeEntity::getNamespace)
			.containsExactlyInAnyOrder(
				tuple(municipalityId, "external-id-type-1", namespace),
				tuple(municipalityId, "external-id-type-2", namespace),
				tuple(municipalityId, "external-id-type-3", namespace));
	}

	@Test
	void delete() {
		// Setup
		final var existingEntity = externalIdTypeRepository.findById(TEST_ID).orElseThrow();

		// Execution
		externalIdTypeRepository.delete(existingEntity);

		// Assertions
		assertThat(externalIdTypeRepository.findById(TEST_ID)).isNotPresent();
	}
}
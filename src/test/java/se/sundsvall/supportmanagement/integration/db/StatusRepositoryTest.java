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

import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;

/**
 * StatusRepository tests.
 *
 * @see src/test/resources/db/testdata.sql for data setup.
 */
@SpringBootTest
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class StatusRepositoryTest {
	private static final long TEST_ID = 101;

	@Autowired
	private StatusRepository statusRepository;

	@Test
	void create() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var name = "status-4";
		final var namespace = "namespace-1";
		final var entity = StatusEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace);

		assertThat(statusRepository.findOne(Example.of(entity))).isEmpty();

		// Execution
		statusRepository.save(entity);

		// Assertions
		final var persistedEntity = statusRepository.findOne(Example.of(entity));

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
		final var name = "status-1";
		final var namespace = "namespace-1";
		final var entity = StatusEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace);

		final var existingEntity = statusRepository.findOne(Example.of(entity)).orElseThrow();
		final var newName = "changed-name";

		// Execution
		existingEntity.setName(newName);
		statusRepository.save(existingEntity);

		// Assertions
		assertThat(statusRepository.findOne(Example.of(entity))).isNotPresent();
		final var updatedEntity = statusRepository.findOne(Example.of(entity.withName(newName)));

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

		final var matches = statusRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId);

		assertThat(matches).hasSize(3)
			.extracting(
				StatusEntity::getMunicipalityId,
				StatusEntity::getName,
				StatusEntity::getNamespace)
			.containsExactlyInAnyOrder(
				tuple(municipalityId, "status-1", namespace),
				tuple(municipalityId, "status-2", namespace),
				tuple(municipalityId, "status-3", namespace));
	}

	@Test
	void delete() {
		// Setup
		final var existingEntity = statusRepository.findById(TEST_ID).orElseThrow();

		// Execution
		statusRepository.delete(existingEntity);

		// Assertions
		assertThat(statusRepository.findById(TEST_ID)).isNotPresent();
	}
}
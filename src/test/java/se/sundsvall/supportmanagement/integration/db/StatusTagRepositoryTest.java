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

import se.sundsvall.supportmanagement.integration.db.model.StatusTagEntity;

/**
 * StatusTagRepository tests.
 *
 * @see src/test/resources/db/testdata.sql for data setup.
 */
@SpringBootTest
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class StatusTagRepositoryTest {
	private static final long TEST_ID = 101;

	@Autowired
	private StatusTagRepository statusTagRepository;

	@Test
	void create() {

		final var municipalityId = "municipalityId-1";
		final var name = "status-4";
		final var namespace = "namespace-1";
		final var tagEntity = StatusTagEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace);

		assertThat(statusTagRepository.findOne(Example.of(tagEntity))).isEmpty();

		// Execution
		statusTagRepository.save(tagEntity);

		// Assertions
		final var persistedEntity = statusTagRepository.findOne(Example.of(tagEntity));

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
		final var tagEntity = StatusTagEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace);

		final var existingEntity = statusTagRepository.findOne(Example.of(tagEntity)).orElseThrow();
		final var newTagName = "changed-name";

		// Execution
		existingEntity.setName(newTagName);
		statusTagRepository.save(existingEntity);

		// Assertions
		assertThat(statusTagRepository.findOne(Example.of(tagEntity))).isNotPresent();
		final var updatedEntity = statusTagRepository.findOne(Example.of(tagEntity.withName(newTagName)));

		assertThat(updatedEntity).isPresent();
		assertThat(updatedEntity.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(updatedEntity.get().getName()).isEqualTo(newTagName);
		assertThat(updatedEntity.get().getNamespace()).isEqualTo(namespace);
		assertThat(updatedEntity.get().getModified()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
	}

	@Test
	void findAllByNamespaceAndMunicipalityId() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var namespace = "namespace-1";

		final var matches = statusTagRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId);

		assertThat(matches).hasSize(3)
			.extracting(
				StatusTagEntity::getMunicipalityId,
				StatusTagEntity::getName,
				StatusTagEntity::getNamespace)
			.containsExactlyInAnyOrder(
				tuple(municipalityId, "status-1", namespace),
				tuple(municipalityId, "status-2", namespace),
				tuple(municipalityId, "status-3", namespace));
	}

	@Test
	void delete() {

		// Setup
		final var existingTagEntity = statusTagRepository.findById(TEST_ID).orElseThrow();

		// Execution
		statusTagRepository.delete(existingTagEntity);

		// Assertions
		assertThat(statusTagRepository.findById(TEST_ID)).isNotPresent();
	}
}

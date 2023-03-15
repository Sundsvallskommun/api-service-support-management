package se.sundsvall.supportmanagement.integration.db;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.OffsetDateTime;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.supportmanagement.integration.db.model.TagValidationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.TagType;

/**
 * TagValidationRepository tests.
 *
 * @see src/test/resources/db/testdata.sql for data setup.
 */
@SpringBootTest
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class TagValidationRepositoryTest {
	private static final long TEST_ID = 100;

	@Autowired
	private TagValidationRepository tagValidationRepository;

	@Test
	void create() {

		final var municipalityId = "municipalityId-1";
		final var namespace = "namespace-1";
		final var type = TagType.EXTERNAL_ID_TYPE;
		final var entity = TagValidationEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withType(type)
			.withValidated(true);

		assertThat(tagValidationRepository.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)).isEmpty();

		// Execution
		tagValidationRepository.save(entity);

		// Assertions
		final var match = tagValidationRepository.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type);

		assertThat(match).isPresent();
		assertThat(match.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(match.get().getNamespace()).isEqualTo(namespace);
		assertThat(match.get().getType()).isEqualTo(type);
		assertThat(match.get().isValidated()).isTrue();
		assertThat(match.get().getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(match.get().getModified()).isNull();
	}

	@Test
	@Transactional
	void update() {

		// Setup
		final var municipalityId = "municipalityId-1";
		final var namespace = "namespace-1";
		final var type = TagType.CATEGORY;

		final var existingEntity = tagValidationRepository.getReferenceById(TEST_ID);
		final var newNamespace = "changed-namespace";
		final var newValidationSetting = false;

		// Assertions before execution
		assertThat(existingEntity.isValidated()).isTrue();

		// Execution
		existingEntity.withNamespace(newNamespace).withValidated(newValidationSetting);
		tagValidationRepository.save(existingEntity);

		// Assertions after execution
		assertThat(tagValidationRepository.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)).isNotPresent();

		final var updatedEntity = tagValidationRepository.findByNamespaceAndMunicipalityIdAndType(newNamespace, municipalityId, type);
		assertThat(updatedEntity).isPresent();
		assertThat(updatedEntity.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(updatedEntity.get().getNamespace()).isEqualTo(newNamespace);
		assertThat(updatedEntity.get().getType()).isEqualTo(type);
		assertThat(updatedEntity.get().isValidated()).isFalse();
		assertThat(updatedEntity.get().getModified()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
	}

	@Test
	void findByNamespaceAndMunicipalityIdAndType() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var namespace = "namespace-2";
		final var type = TagType.STATUS;

		// Assertions
		final var match = tagValidationRepository.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type);

		assertThat(match).isPresent();
		assertThat(match.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(match.get().getNamespace()).isEqualTo(namespace);
		assertThat(match.get().getType()).isEqualTo(type);
		assertThat(match.get().isValidated()).isTrue();
	}

	@Test
	void findAllByNamespaceAndMunicipalityId() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var namespace = "namespace-1";

		final var matches = tagValidationRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId);

		assertThat(matches).hasSize(2)
			.extracting(
				TagValidationEntity::getMunicipalityId,
				TagValidationEntity::getNamespace,
				TagValidationEntity::getType,
				TagValidationEntity::isValidated)
			.containsExactlyInAnyOrder(
				tuple(municipalityId, namespace, TagType.CATEGORY, true),
				tuple(municipalityId, namespace, TagType.TYPE, false));
	}

	@Test
	void delete() {

		// Setup
		final var existingTagEntity = tagValidationRepository.findById(TEST_ID).orElseThrow();

		// Execution
		tagValidationRepository.delete(existingTagEntity);

		// Assertions
		assertThat(tagValidationRepository.findById(TEST_ID)).isNotPresent();
	}
}

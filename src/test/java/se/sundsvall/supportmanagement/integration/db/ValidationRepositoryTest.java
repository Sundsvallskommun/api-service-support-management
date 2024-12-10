package se.sundsvall.supportmanagement.integration.db;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.ValidationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;

/**
 * ValidationRepository tests.
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
class ValidationRepositoryTest {

	private static final long TEST_ID = 100;

	@Autowired
	private ValidationRepository validationRepository;

	@Test
	void create() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var namespace = "namespace-1";
		final var type = EntityType.EXTERNAL_ID_TYPE;
		final var entity = ValidationEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withType(type)
			.withValidated(true);

		assertThat(validationRepository.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)).isEmpty();

		// Execution
		validationRepository.save(entity);

		// Assertions
		final var match = validationRepository.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type);

		assertThat(match).isPresent();
		assertThat(match.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(match.get().getNamespace()).isEqualTo(namespace);
		assertThat(match.get().getType()).isEqualTo(type);
		assertThat(match.get().isValidated()).isTrue();
		assertThat(match.get().getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(match.get().getModified()).isNull();
	}

	@Test
	void update() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var namespace = "namespace-1";
		final var type = EntityType.CATEGORY;

		final var existingEntity = validationRepository.getReferenceById(TEST_ID);
		final var newNamespace = "changed-namespace";
		final var newValidationSetting = false;

		// Assertions before execution
		assertThat(existingEntity.isValidated()).isTrue();

		// Execution
		existingEntity.withNamespace(newNamespace).withValidated(newValidationSetting);
		validationRepository.save(existingEntity);

		// Assertions after execution
		assertThat(validationRepository.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)).isNotPresent();

		final var updatedEntity = validationRepository.findByNamespaceAndMunicipalityIdAndType(newNamespace, municipalityId, type);
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
		final var type = EntityType.STATUS;

		// Assertions
		final var match = validationRepository.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type);

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

		final var matches = validationRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId);

		assertThat(matches).hasSize(2)
			.extracting(
				ValidationEntity::getMunicipalityId,
				ValidationEntity::getNamespace,
				ValidationEntity::getType,
				ValidationEntity::isValidated)
			.containsExactlyInAnyOrder(
				tuple(municipalityId, namespace, EntityType.CATEGORY, true),
				tuple(municipalityId, namespace, EntityType.TYPE, false));
	}

	@Test
	void delete() {
		// Setup
		final var existingEntity = validationRepository.findById(TEST_ID).orElseThrow();

		// Execution
		validationRepository.delete(existingEntity);

		// Assertions
		assertThat(validationRepository.findById(TEST_ID)).isNotPresent();
	}
}

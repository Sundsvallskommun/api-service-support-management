package se.sundsvall.supportmanagement.integration.db;

import static java.lang.Integer.parseInt;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.RevisionEntity;

/**
 * Revision repository tests.
 *
 * @see src/test/resources/db/testdata-junit.sql for data setup.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class RevisionRepositoryTest {

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "2281";

	private static final String ENTITY_ID = "9791682e-4ba8-4f3a-857a-54e14836a53b";

	@Autowired
	private RevisionRepository repository;

	@Test
	void create() {
		final var result = repository.save(RevisionEntity.create()
			.withEntityId(randomUUID().toString())
			.withEntityType(ErrandEntity.class.getSimpleName())
			.withSerializedSnapshot("{}")
			.withVersion(0));

		assertThat(result).isNotNull();
		assertThat(result.getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(result.getEntityType()).isEqualTo("ErrandEntity");
		assertThat(isValidUUID(result.getId())).isTrue();
		assertThat(isValidUUID(result.getEntityId())).isTrue();
	}

	@Test
	void createFailsDueToUniqueConstraintViolation() {

		// Arrange
		final var entityId = randomUUID().toString();
		final var version = "2";
		final var entity1 = RevisionEntity.create()
			.withEntityId(entityId)
			.withEntityType(ErrandEntity.class.getSimpleName())
			.withSerializedSnapshot("{}")
			.withVersion(parseInt(version));
		final var entity2 = RevisionEntity.create()
			.withEntityId(entityId)
			.withEntityType(ErrandEntity.class.getSimpleName())
			.withSerializedSnapshot("{}")
			.withVersion(parseInt(version));

		repository.saveAndFlush(entity1);

		// Second save. Will fail due to to unique constraint violation on entityId and version.
		final var exception = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(entity2));

		assertThat(exception.getMessage()).contains("Duplicate entry", version, entityId, "for key 'uq_entity_id_version");
	}

	@Test
	void findByNamespaceAndMunicipalityIdAndEntityIdAndVersion() {
		// Setup
		final var version = 3;

		final var revision = repository.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ENTITY_ID, version);

		assertThat(revision).isPresent();
		assertThat(revision.get().getEntityId()).isEqualTo(ENTITY_ID);
		assertThat(revision.get().getVersion()).isEqualTo(version);
	}

	@Test
	void findByEntityIdAndVersionNotFound() {
		// Setup
		final var version = 666;

		final var revision = repository.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(NAMESPACE, MUNICIPALITY_ID, ENTITY_ID, version);

		assertThat(revision).isEmpty();
	}

	@Test
	void findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc() {
		final var revision = repository.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, ENTITY_ID);

		assertThat(revision).isPresent();
		assertThat(revision.get().getEntityId()).isEqualTo(ENTITY_ID);
		assertThat(revision.get().getVersion()).isEqualTo(5);
	}

	@Test
	void findFirstByEntityIdOrderByVersionDescNotFound() {
		final var revision = repository.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(NAMESPACE, MUNICIPALITY_ID, "does-not-exist");

		assertThat(revision).isEmpty();
	}

	@Test
	void findByEntityId() {
		final var versionList = repository.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, ENTITY_ID);

		assertThat(versionList)
			.hasSize(5)
			.extracting(RevisionEntity::getVersion)
			.containsExactlyInAnyOrder(1, 2, 3, 4, 5);
	}

	@Test
	void findByEntityIdNotFound() {
		final var versionList = repository.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, "does-not-exist");

		assertThat(versionList).isNotNull().isEmpty();
	}

	private boolean isValidUUID(final String value) {
		try {
			UUID.fromString(String.valueOf(value));
		} catch (final Exception e) {
			return false;
		}

		return true;
	}

}

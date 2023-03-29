package se.sundsvall.supportmanagement.integration.db;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.supportmanagement.integration.db.model.RoleEntity;

/**
 * RoleRepository tests.
 *
 * @see src/test/resources/db/testdata.sql for data setup.
 */
@SpringBootTest
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class RoleRepositoryTest {

	private static final long TEST_ID = 101;

	@Autowired
	private RoleRepository roleRepository;

	@Test
	void create() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var name = "role-4";
		final var namespace = "namespace-1";
		final var entity = RoleEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace);

		assertThat(roleRepository.findOne(Example.of(entity))).isEmpty();

		// Execution
		roleRepository.save(entity);

		// Assertions
		final var persistedEntity = roleRepository.findOne(Example.of(entity));

		assertThat(persistedEntity).isPresent();
		assertThat(persistedEntity.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(persistedEntity.get().getName()).isEqualTo(name);
		assertThat(persistedEntity.get().getNamespace()).isEqualTo(namespace);
		assertThat(persistedEntity.get().getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(persistedEntity.get().getModified()).isNull();
	}

	@Test
	void update() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var name = "role-1";
		final var namespace = "namespace-1";
		final var entity = RoleEntity.create()
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withNamespace(namespace);

		final var existingEntity = roleRepository.findOne(Example.of(entity)).orElseThrow();
		final var newName = "changed-name";

		// Execution
		existingEntity.setName(newName);
		roleRepository.save(existingEntity);

		// Assertions
		assertThat(roleRepository.findOne(Example.of(entity))).isNotPresent();
		final var updatedEntity = roleRepository.findOne(Example.of(entity.withName(newName)));

		assertThat(updatedEntity).isPresent();
		assertThat(updatedEntity.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(updatedEntity.get().getName()).isEqualTo(newName);
		assertThat(updatedEntity.get().getNamespace()).isEqualTo(namespace);
		assertThat(updatedEntity.get().getModified()).isCloseTo(now(), within(2, SECONDS));
	}

	@Test
	void getByNamespaceAndMunicipalityIdAndName() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var namespace = "namespace-1";
		final var name = "role-3";

		// Execution
		final var entity = roleRepository.getByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, name);

		// Assertions
		assertThat(entity).isNotNull();
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getName()).isEqualTo(name);
	}

	@Test
	void existsByNamespaceAndMunicipalityIdAndName() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var namespace = "namespace-1";
		final var existingRoleName = "role-3";
		final var nonExistingRoleName = "role-4";

		// Execution & assertion
		assertThat(roleRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, existingRoleName)).isTrue();
		assertThat(roleRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, nonExistingRoleName)).isFalse();
	}

	@Test
	void findAllByNamespaceAndMunicipalityId() {
		// Setup
		final var municipalityId = "municipalityId-1";
		final var namespace = "namespace-1";

		final var matches = roleRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId);

		assertThat(matches).hasSize(3)
			.extracting(
				RoleEntity::getMunicipalityId,
				RoleEntity::getName,
				RoleEntity::getNamespace)
			.containsExactlyInAnyOrder(
				tuple(municipalityId, "role-1", namespace),
				tuple(municipalityId, "role-2", namespace),
				tuple(municipalityId, "role-3", namespace));
	}

	@Test
	void delete() {
		// Setup
		final var existingEntity = roleRepository.findById(TEST_ID).orElseThrow();

		// Execution
		roleRepository.deleteByNamespaceAndMunicipalityIdAndName(existingEntity.getNamespace(), existingEntity.getMunicipalityId(), existingEntity.getName());

		// Assertions
		assertThat(roleRepository.findById(TEST_ID)).isNotPresent();
	}
}

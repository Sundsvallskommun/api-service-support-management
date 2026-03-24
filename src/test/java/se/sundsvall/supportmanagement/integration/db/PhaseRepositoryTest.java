package se.sundsvall.supportmanagement.integration.db;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.PhaseEntity;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * PhaseRepository tests.
 *
 * @see <a href="file:src/test/resources/db/scripts/testdata-junit.sql">testdata-junit.sql</a> for data setup.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class PhaseRepositoryTest {

	@Autowired
	private PhaseRepository phaseRepository;

	@Test
	void create() {
		final var entity = PhaseEntity.create()
			.withMunicipalityId("2281")
			.withNamespace("namespace-1")
			.withName("phase-new")
			.withDisplayName("New Phase")
			.withDescription("A new phase")
			.withPhaseOrder(10)
			.withAllowedStatuses(List.of("OPEN"));

		final var saved = phaseRepository.save(entity);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getName()).isEqualTo("phase-new");
		assertThat(saved.getDisplayName()).isEqualTo("New Phase");
		assertThat(saved.getDescription()).isEqualTo("A new phase");
		assertThat(saved.getPhaseOrder()).isEqualTo(10);
		assertThat(saved.getAllowedStatuses()).containsExactly("OPEN");
		assertThat(saved.getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(saved.getModified()).isNull();
	}

	@Test
	void findAllByNamespaceAndMunicipalityId() {
		final var result = phaseRepository.findAllByNamespaceAndMunicipalityId("namespace-1", "2281");

		assertThat(result).hasSize(3)
			.extracting(PhaseEntity::getName)
			.containsExactlyInAnyOrder("phase-1", "phase-2", "phase-3");
	}

	@Test
	void findAllByNamespaceAndMunicipalityIdNoMatch() {
		final var result = phaseRepository.findAllByNamespaceAndMunicipalityId("namespace-1", "9999");

		assertThat(result).isEmpty();
	}

	@Test
	void findByIdAndNamespaceAndMunicipalityId() {
		final var result = phaseRepository.findByIdAndNamespaceAndMunicipalityId("phase-id-1", "namespace-1", "2281");

		assertThat(result).isPresent();
		assertThat(result.get().getName()).isEqualTo("phase-1");
		assertThat(result.get().getDisplayName()).isEqualTo("Phase 1");
		assertThat(result.get().getAllowedStatuses()).containsExactlyInAnyOrder("NEW", "IN_PROGRESS");
		assertThat(result.get().getTransitions()).hasSize(1);
	}

	@Test
	void findByIdAndNamespaceAndMunicipalityIdNoMatch() {
		final var result = phaseRepository.findByIdAndNamespaceAndMunicipalityId("phase-id-1", "namespace-2", "2281");

		assertThat(result).isEmpty();
	}

	@Test
	void existsByIdAndNamespaceAndMunicipalityId() {
		assertThat(phaseRepository.existsByIdAndNamespaceAndMunicipalityId("phase-id-1", "namespace-1", "2281")).isTrue();
		assertThat(phaseRepository.existsByIdAndNamespaceAndMunicipalityId("phase-id-1", "namespace-2", "2281")).isFalse();
		assertThat(phaseRepository.existsByIdAndNamespaceAndMunicipalityId("non-existent", "namespace-1", "2281")).isFalse();
	}

	@Test
	void existsByNamespaceAndMunicipalityIdAndName() {
		assertThat(phaseRepository.existsByNamespaceAndMunicipalityIdAndName("namespace-1", "2281", "phase-1")).isTrue();
		assertThat(phaseRepository.existsByNamespaceAndMunicipalityIdAndName("namespace-1", "2281", "non-existent")).isFalse();
	}

	@Test
	void deleteByIdAndNamespaceAndMunicipalityId() {
		assertThat(phaseRepository.findByIdAndNamespaceAndMunicipalityId("phase-id-3", "namespace-1", "2281")).isPresent();

		phaseRepository.deleteByIdAndNamespaceAndMunicipalityId("phase-id-3", "namespace-1", "2281");

		assertThat(phaseRepository.findByIdAndNamespaceAndMunicipalityId("phase-id-3", "namespace-1", "2281")).isEmpty();
	}

	@Test
	void update() {
		final var entity = phaseRepository.findByIdAndNamespaceAndMunicipalityId("phase-id-1", "namespace-1", "2281").orElseThrow();

		entity.setName("updated-name");
		entity.setDisplayName("Updated Display");
		phaseRepository.save(entity);
		phaseRepository.flush();

		final var updated = phaseRepository.findByIdAndNamespaceAndMunicipalityId("phase-id-1", "namespace-1", "2281").orElseThrow();

		assertThat(updated.getName()).isEqualTo("updated-name");
		assertThat(updated.getDisplayName()).isEqualTo("Updated Display");
		assertThat(updated.getModified()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
	}
}

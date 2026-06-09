package se.sundsvall.supportmanagement.integration.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the time_measurement orphan bug: when an errand status is changed and persisted directly via
 * {@link ErrandsRepository#save} (as the scheduler workers do, e.g. EmailReaderWorker / MessageExchangeSyncService),
 * the
 * {@code ErrandListener#onUpdate} lifecycle callback adds a new TimeMeasurementEntity to the unidirectional
 * {@code @OneToMany @JoinColumn} collection. The FK (errand_id) is owned solely by the collection, so the row is
 * inserted but errand_id is left
 * null.
 */
@SpringBootTest
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
@Transactional
class ErrandTimeMeasurementListenerTest {

	@Autowired
	private ErrandsRepository errandsRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	void statusChangeViaRepositorySaveDoesNotOrphanTimeMeasurement() {

		// Setup — create a fresh errand. @PrePersist adds the first time measure (FK set correctly on create).
		final var errandEntity = ErrandEntity.create()
			.withNamespace("NAMESPACE.1")
			.withMunicipalityId("2281")
			.withErrandNumber("TM-ORPHAN-1")
			.withTitle("title")
			.withCategory("category")
			.withType("type")
			.withStatus("OPEN")
			.withPriority("HIGH")
			.withAssignedUserId("user-1");

		final var persisted = errandsRepository.save(errandEntity);
		errandsRepository.flush();
		final var errandId = persisted.getId();

		// Detach so the reload fires @PostLoad (sets tempPreviousStatus) — exactly like a worker loading the errand.
		entityManager.clear();

		// Execution — mimic EmailReaderWorker/ MessageExchangeSyncService
		final var reloaded = errandsRepository.findById(errandId).orElseThrow();
		reloaded.setStatus("SOLVED");
		errandsRepository.save(reloaded);
		errandsRepository.flush();

		// Assertions — no orphaned time_measurement rows must exist.
		// (The native queries below autoflush the pending insert added by the @PreUpdate listener.)
		final var orphanCount = ((Number) entityManager
			.createNativeQuery("SELECT COUNT(*) FROM time_measurement WHERE errand_id IS NULL")
			.getSingleResult()).longValue();

		final var rowsForErrand = ((Number) entityManager
			.createNativeQuery("SELECT COUNT(*) FROM time_measurement WHERE errand_id = :id")
			.setParameter("id", errandId)
			.getSingleResult()).longValue();

		assertThat(orphanCount).as("time_measurement rows with null errand_id").isZero();
		// One measure from create (OPEN), one from the status change (SOLVED)
		assertThat(rowsForErrand).as("time_measurement rows linked to the errand").isEqualTo(2);

		final var errandTimeMeasures = errandsRepository.findById(errandId).orElseThrow().getTimeMeasures();
		assertThat(errandTimeMeasures).as("time measures reachable from errand").hasSize(2);
	}
}

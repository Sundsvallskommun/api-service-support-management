package se.sundsvall.supportmanagement.integration.db;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;

/**
 * The log is read per errand rather than per instance, and the entries with no instance are the reason why: they are
 * what explains that no process started at all.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql",
	"/db/scripts/testdata-junit-process.sql"
})
class ErrandProcessActivityRepositoryTest {

	private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "occurredAt");

	@Autowired
	private ErrandProcessActivityRepository errandProcessActivityRepository;

	/**
	 * Reads a moment out of the test data the way the entities do: as a wall clock in the default zone of the JVM. Taking
	 * it off the clock of the machine instead would leave every assertion here depending on the build and the database
	 * agreeing on a time zone, which they do not.
	 */
	private static OffsetDateTime at(final String wallClock) {
		return LocalDateTime.parse(wallClock).atZone(systemDefault()).toOffsetDateTime();
	}

	@Test
	@DisplayName("Verification that reading per errand reaches the entries that have no process instance to be found by")
	void findByErrandId() {
		assertThat(errandProcessActivityRepository.findByErrandId("ERRAND_ID-1", PageRequest.of(0, 10, NEWEST_FIRST)))
			.extracting(ErrandProcessActivityEntity::getId)
			.containsExactly("epa-config-1", "epa-phase-1", "epa-task-1");
	}

	@Test
	@DisplayName("Verification that narrowing to one instance leaves the instanceless entries out, which is why it cannot be the only way to read")
	void findByErrandIdAndErrandProcessId() {
		assertThat(errandProcessActivityRepository.findByErrandIdAndErrandProcessId("ERRAND_ID-1", "ep-live-1", PageRequest.of(0, 10, NEWEST_FIRST)))
			.extracting(ErrandProcessActivityEntity::getId)
			.containsExactly("epa-phase-1", "epa-task-1");
	}

	@Test
	@DisplayName("Verification that an errand already carrying the entry is recognised, so a jammed errand is reported once per window instead of once per discarded event")
	void existsByErrandIdAndActivityTypeAndSeverityAndCreatedAfter() {
		final var windowStart = at("2026-01-01T11:50:00");

		assertThat(errandProcessActivityRepository.existsByErrandIdAndActivityTypeAndSeverityAndCreatedAfter("ERRAND_ID-1", "CONFIG", ERROR, windowStart)).isTrue();
		assertThat(errandProcessActivityRepository.existsByErrandIdAndActivityTypeAndSeverityAndCreatedAfter("ERRAND_ID-2", "CONFIG", ERROR, windowStart)).isFalse();
	}

	@Test
	@DisplayName("Verification that retention runs on the clock of SM, not on the one the process reported")
	void findByCreatedBeforeOrderByCreatedAsc() {
		assertThat(errandProcessActivityRepository.findByCreatedBeforeOrderByCreatedAsc(at("2025-06-01T00:00:00"), PageRequest.of(0, 100)))
			.extracting(ErrandProcessActivityEntity::getId)
			.containsExactly("epa-config-2");
	}

	@Test
	@DisplayName("Verification that an entry can be written before any process instance exists")
	void anActivityWithoutAProcessInstanceCanBeSaved() {
		final var saved = errandProcessActivityRepository.saveAndFlush(ErrandProcessActivityEntity.create()
			.withErrandId("ERRAND_ID-3")
			.withActivityType("CONFIG")
			.withSeverity(ERROR)
			.withMessage("Two labels resolve to different process keys")
			.withOccurredAt(now(systemDefault())));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getErrandProcessId()).isNull();
		assertThat(errandProcessActivityRepository.findById(saved.getId())).isPresent();
	}
}

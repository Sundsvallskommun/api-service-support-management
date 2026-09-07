package se.sundsvall.supportmanagement.integration.db;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;

import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * What is worth pinning down against a real database is which rows a delivery run is allowed to see, and what the
 * emergency brake counts. Both are questions about time and about state at once, and neither reads as intended from the
 * method name alone.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql",
	"/db/scripts/testdata-junit-process.sql"
})
class ProcessEventOutboxRepositoryTest {

	@Autowired
	private ProcessEventOutboxRepository processEventOutboxRepository;

	/**
	 * Reads a moment out of the test data the way the entities do: as a wall clock in the default zone of the JVM. Taking
	 * it off the clock of the machine instead would leave every assertion here depending on the build and the database
	 * agreeing on a time zone, which they do not.
	 */
	private static OffsetDateTime at(final String wallClock) {
		return LocalDateTime.parse(wallClock).atZone(systemDefault()).toOffsetDateTime();
	}

	@Test
	@DisplayName("Verification that a run takes the rows of its own consumer, leaves the delivered ones alone and skips a row too young to be settled")
	void findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore() {
		assertThat(processEventOutboxRepository.findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore("pw-alkt", at("2026-01-01T11:55:00"), PageRequest.of(0, 100)))
			.extracting(ProcessEventOutboxEntity::getId)
			.containsExactly("peo-waiting");
	}

	@Test
	@DisplayName("Verification that the batch limit holds, so one engine being down cannot spend the whole run")
	void findByProcessServiceHonoursTheBatchLimit() {
		assertThat(processEventOutboxRepository.findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore("pw-alkt", at("2026-01-01T12:00:00"), PageRequest.of(0, 100)))
			.hasSize(2);

		assertThat(processEventOutboxRepository.findByProcessServiceAndDeliveredAtIsNullAndCreatedBefore("pw-alkt", at("2026-01-01T12:00:00"), PageRequest.of(0, 1)))
			.hasSize(1);
	}

	@Test
	@DisplayName("Verification that the emergency brake counts what reached the process engine, not what is queued for it")
	void countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter() {
		assertThat(processEventOutboxRepository.countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter("ERRAND_ID-1", at("2026-01-01T11:50:00"))).isOne();
	}

	@Test
	@DisplayName("Verification that a delivery outage cannot trip the brake by itself, however many rows pile up undelivered")
	void countLeavesUndeliveredRowsOutOfTheWindow() {
		assertThat(processEventOutboxRepository.countByErrandIdAndDeliveredAtIsNotNullAndCreatedAfter("ERRAND_ID-1", at("2025-01-01T00:00:00"))).isEqualTo(2);
	}

	@Test
	@DisplayName("Verification that health is read from the oldest undelivered row, which is what makes an age and not a count the condition")
	void findByDeliveredAtIsNullOrderByCreatedAsc() {
		assertThat(processEventOutboxRepository.findByDeliveredAtIsNullOrderByCreatedAsc(PageRequest.of(0, 1)))
			.extracting(ProcessEventOutboxEntity::getId)
			.containsExactly("peo-other-consumer");
	}
}
